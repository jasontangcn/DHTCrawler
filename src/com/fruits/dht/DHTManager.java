package com.fruits.dht;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DHTManager {
    public static String selfNodeId;
    public static Node selfNode;

    public static String LISTENER_DOMAIN; // "127.0.0.1"
    public static int LISTENER_PORT; // 6881

    private final UDPServer udpServer;

    private RoutingTable routingTable;

    // transaction id -> Query
    // used to parse KMessage.

    // find_node query could spawn multi sub find_node request with the same transaction id,
    // so the previous one will overrided and only the last one left,
    // no problem, but the map entry in "queries" only used for identifying the type of the request.
    public final Map<String, KMessage.Query> queries = new HashMap<String, KMessage.Query>();

    // nodeId -> PingTask
    public final static Map<String, PingTask> pingTasks = new HashMap<String, PingTask>();

    // transactionId -> FindNodeTask
    public Map<String, FindNodeTask> findNodeTasks = new HashMap<String, FindNodeTask>();

    // transactionId -> FindNodeTask
    private Map<String, GetPeersTask> getPeersTasks = new HashMap<String, GetPeersTask>();

    // announce_peer request manipulate "infohashPeerList".
    // infohash-> peer(endpoint not node) who store the infohash.

    // two ways to get peer list
    // 1. GetPeersResponse -> do not know node id.
    // 2. AnnouncePeerQuery -> know node id,
    //
    // So only use InetSocketAddress without node id.
    private Map<String, List<InetSocketAddress>> infohashPeerList = new HashMap<String, List<InetSocketAddress>>();

    static {
        // Initialize it by JVM options
        Properties props = System.getProperties();

        LISTENER_DOMAIN = props.getProperty("listener.domain");
        LISTENER_PORT = Integer.parseInt(props.getProperty("listener.port"));

        try{
            selfNodeId = Utils.generateNodeId();
        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }

        selfNode = new Node(selfNodeId, new InetSocketAddress(LISTENER_DOMAIN, LISTENER_PORT));
    }

    public DHTManager() throws IOException {
        this.udpServer = new UDPServer(this);
        this.routingTable = new RoutingTable();
        // add myself as the first node.
        routingTable.putNodeInBucket(selfNode);
    }

    // init the RoutingTable
    public void initRoutingTable() throws IOException, UnsupportedEncodingException {
        // 1. build a routing table.
        // 2. initialize the routing table by finding node selfNodeId(or known node id e.g. "67d0515bcf1e9ddb25ca909135c2c684b41f1dbe"),
        //    router.bittorrent.com:6881、 dht.transmissionbt.com:6881
        // 3.
        String transactionId = Utils.generateTransactionId();
        //ByteBuffer findNodeRequest = createFindNodeRequest(transactionId, selfNodeId, "e5591e20a8f02398a9948c4e35ccfc6b3da21a56");
        //Datagram datagram = new Datagram(new InetSocketAddress("dht.transmissionbt.com", 6881), findNodeRequest);

        Node startNode = new Node(null/* do not know the node id */, new InetSocketAddress("dht.transmissionbt.com", 6881));
        findNode(startNode, selfNode.getId()); // find myself to initialize the routing table.
    }

    public void findNode(Node closerNode, String targetNodeId) throws IOException {
        String transactionId = Utils.generateTransactionId();
        FindNodeTask findNodeTask = new FindNodeTask(transactionId, targetNodeId);
        findNodeTasks.put(transactionId, findNodeTask);
        // TODO(NOTICE): the first node may not have a nodeId, it may be router.bittorrent.com:6881 or dht.transmissionbt.com:6881.
        findNodeTask.putQueryingNode(closerNode);
        FindNodeThread findNodeThread = new FindNodeThread(findNodeTask, this);
        new Thread(findNodeThread).start();
    }

    public void getPeers(Node closerNode, String infohash) throws IOException {
        String transactionId = Utils.generateTransactionId();
        GetPeersTask getPeersTask = new GetPeersTask(transactionId, infohash);
        getPeersTasks.put(transactionId, getPeersTask);
        getPeersTask.putQueryingNode(closerNode);
        GetPeersThread getPeersThread = new GetPeersThread(getPeersTask, this);
        new Thread(getPeersThread).start();
    }

    // TODO: any request or response received, need to update the routing table.
    public void handleMessage(KMessage message) throws IOException {
        if(message == null)
            return;

        if (message instanceof KMessage.PingQuery) {
            KMessage.PingQuery pingQuery = (KMessage.PingQuery) message;
            KMessage.PingResponse pingResponse = new KMessage.PingResponse(pingQuery.getT(), DHTManager.selfNodeId);
            ByteBuffer data = pingResponse.bencode();

            // TODO: only remote address could be used, is it correct?
            Datagram datagram = new Datagram(pingQuery.getRemoteAddress(), data);
            try {
                udpServer.addDatagramToSend(datagram);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // TODO: should I put peer in bucket?
            //       how could I know the endpoint of peer? remote address?
        } else if (message instanceof KMessage.PingResponse) {
            // logic once receiving PingResponse
            // 1. find the relative PingQuery and mark it as 'alive'.
            KMessage.PingResponse pingResponse = (KMessage.PingResponse) message;
            String t = pingResponse.getT();
            String nodeId = (String) pingResponse.getR(KMessage.KMESSAGE_KEY_ID);
            PingTask pingTask = pingTasks.get(nodeId);
            // transaction id equals -> double check.
            if (pingTask != null && pingTask.getTransactionId().equals(t)) {
                pingTask.setResponseReceived(true);
            }
        } else if(message instanceof KMessage.FindNodeQuery) {
            KMessage.FindNodeQuery findNodeQuery = (KMessage.FindNodeQuery)message;
            String targetNodeId = findNodeQuery.getA(KMessage.KMESSAGE_QUERY_KEY_TARGET);
            // if there is only one node, maybe, it's the node to be found.
            List<Node> closestNodes = this.routingTable.getClosest8Nodes(targetNodeId);
            String nodesString = Utils.createCompactNodesString(closestNodes);
            // TODO: the second argument is correct?
            // nodeString is correct?
            KMessage.FindNodeResponse findNodeResponse = new KMessage.FindNodeResponse(findNodeQuery.getT(), DHTManager.selfNodeId, nodesString);

            Datagram datagram = new Datagram(findNodeQuery.getRemoteAddress(), findNodeResponse.bencode());
            try {
                udpServer.addDatagramToSend(datagram);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else if(message instanceof KMessage.FindNodeResponse) {
            KMessage.FindNodeResponse findNodeResponse = (KMessage.FindNodeResponse)message;
            String nodesString = (String)findNodeResponse.getR(KMessage.KMESSAGE_RESPONSE_KEY_NODES);

            FindNodeTask findNodeTask = this.findNodeTasks.get(findNodeResponse.getT());

            for(Node node : Utils.parseCompactNodes(nodesString)) {
                // TODO(NOTICE)! node must have nodeId, and address.

                findNodeTask.putQueryingNode(node);
                // if have found the target node, do nothing and put it in the querying queue,
                // the FindNodeThread will check the nodes in the queringNodes queue.

                routingTable.putNodeInBucket(node);
            }
        }else if(message instanceof KMessage.GetPeersQuery) {
            KMessage.GetPeersQuery getPeersQuery = (KMessage.GetPeersQuery)message;
            String infohash = getPeersQuery.getA(KMessage.KMESSAGE_QUERY_KEY_INFO_HASH);
            List<InetSocketAddress> peerList = infohashPeerList.get(infohash);

            KMessage.GetPeersResponse getPeersResponse;

            // found the peer list of the infohash.
            if(peerList.size() > 0) {
                // TODO: the second parameter is correct? is it the node id of the get_peers requester?
                // TODO: generate a token?
                getPeersResponse = new KMessage.GetPeersResponse(getPeersQuery.getT(), getPeersQuery.getA(KMessage.KMESSAGE_KEY_ID), "aoeusnth", Utils.createCompactPeerStringList(peerList));
            }else{
                List<Node> closestNodes = this.routingTable.getClosest8Nodes(infohash);
                String nodesString = Utils.createCompactNodesString(closestNodes);
                // TODO: the second parameter is correct? is it the node id of the get_peers requester?
                // TODO: generate a token?
                // nodeString is correct?
                getPeersResponse = new KMessage.GetPeersResponse(getPeersQuery.getT(), getPeersQuery.getA(KMessage.KMESSAGE_KEY_ID), "aoeusnth", nodesString);
            }
            Datagram datagram = new Datagram(getPeersQuery.getRemoteAddress(), getPeersResponse.bencode());
            try {
                udpServer.addDatagramToSend(datagram);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } else if(message instanceof KMessage.GetPeersResponse) {
            KMessage.GetPeersResponse getPeersResponse = (KMessage.GetPeersResponse)message;

            KMessage.GetPeersQuery getPeersQuery = (KMessage.GetPeersQuery) queries.get(getPeersResponse.getT());
            String infohash = getPeersQuery.getA(KMessage.KMESSAGE_QUERY_KEY_INFO_HASH);

            String token = (String)getPeersResponse.getR(KMessage.KMESSAGE_QUERY_KEY_TOKEN);

            // GetPeersRespondedNode : key field: <address>
            GetPeersRespondedNode getPeersRespondedNode = new GetPeersRespondedNode(getPeersResponse.getRemoteAddress(), token);
            GetPeersTask getPeersTask = this.getPeersTasks.get(getPeersResponse.getT());

            getPeersTask.putRespondedNode(getPeersRespondedNode);

            String nodesString = (String)getPeersResponse.getR(KMessage.KMESSAGE_RESPONSE_KEY_NODES);

            if(nodesString != null) {
                for (Node node : Utils.parseCompactNodes(nodesString)) {
                    getPeersTask.putQueryingNode(node);

                    routingTable.putNodeInBucket(node);
                }
            }else{
                List<String> peerStringList = (List<String>)getPeersResponse.getR(KMessage.KMESSAGE_RESPONSE_KEY_VALUES);
                List<InetSocketAddress> peers = Utils.parseCompactPeers(peerStringList);
                for(InetSocketAddress peer : peers)
                    putPeer(infohash, peer);

                /*
                for(InetSocketAddress peer : peers) {
                    getPeersTask.putPeer(peer);
                }
                */
                getPeersTask.setFoundSomePeers(true);
            }
        }else if(message instanceof KMessage.AnnouncePeerQuery) {
            KMessage.AnnouncePeerQuery announcePeerQuery = (KMessage.AnnouncePeerQuery)message;
            //announce_peers Query = {"t":"aa", "y":"q", "q":"announce_peer", "a": {"id":"abcdefghij0123456789", "implied_port": 1, "info_hash":"mnopqrstuvwxyz123456", "port": 6881, "token": "aoeusnth"}}
            String nodeId = announcePeerQuery.getA(KMessage.KMESSAGE_KEY_ID);
            String infohash = announcePeerQuery.getA(KMessage.KMESSAGE_QUERY_KEY_INFO_HASH);
            String token = announcePeerQuery.getA(KMessage.KMESSAGE_QUERY_KEY_TOKEN);
            int port = Integer.parseInt(announcePeerQuery.getA(KMessage.KMESSAGE_QUERY_KEY_PORT));

            // TODO: verify the token
            // TODO: remoteAddress or ?
            //Node node  = new Node(nodeId, new InetSocketAddress(announcePeerQuery.getRemoteAddress().getAddress(), port));

            this.putPeer(infohash, new InetSocketAddress(announcePeerQuery.getRemoteAddress().getAddress(), port));

            KMessage.AnnouncePeerResponse announcePeerResponse = new KMessage.AnnouncePeerResponse(announcePeerQuery.getT(), DHTManager.selfNodeId);
            Datagram datagram = new Datagram(announcePeerQuery.getRemoteAddress(), announcePeerResponse.bencode());
            try {
                udpServer.addDatagramToSend(datagram);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }else if(message instanceof KMessage.AnnouncePeerResponse) {
            KMessage.AnnouncePeerResponse announcePeerResponse = (KMessage.AnnouncePeerResponse)message;
            // TODO:
            // Do nothing except updating the routing table?
        }
    }

    public UDPServer getUdpServer() {
        return this.udpServer;
    }

    public KMessage.Query getQuery(String transactionId) {
        return this.queries.get(transactionId);
    }

    public void putQuery(String transactionId, KMessage.Query query) {
        this.queries.put(transactionId, query);
    }

    public void removeQuery(String transactionId) {
        this.queries.remove(transactionId);
    }

    public Map<String, KMessage.Query> getQueries() {
        return this.queries;
    }

    public void removeFindNodeTask(String transactionId) {
        this.findNodeTasks.remove(transactionId);
    }

    public void removeGetPeersTask(String transactionId) {
        this.getPeersTasks.remove(transactionId);
    }

    public void putPeer(String infohash, InetSocketAddress peer) {
        List<InetSocketAddress> peerList = this.infohashPeerList.get(infohash);
        if(peerList == null)
            peerList = new ArrayList<InetSocketAddress>();
        peerList.add(peer);
    }

    public static void main(String[] args) {

    }
}
