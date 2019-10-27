package com.fruits.dht;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DHTManager {
    private final UDPServer udpServer;

    private RoutingTable routingTable = new RoutingTable();

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

    // announce_peer request manipulate "infohashs".
    // infohash-> nodes who store the infohash.
    private Map<String, List<Node>> infohashs = new HashMap<String, List<Node>>();

    public DHTManager() throws IOException {
        this.udpServer = new UDPServer(this);
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
            KMessage.PingResponse pingResponse = new KMessage.PingResponse(pingQuery.getT(), DHTClient.selfNodeId);
            ByteBuffer data = pingResponse.bencode();
            Datagram datagram = new Datagram(pingQuery.getRemoteAddress(), data);
            try {
                udpServer.addDatagramToSend(datagram);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (message instanceof KMessage.PingResponse) {
            // logic once receiving PingResponse
            // 1. find the relative PingQuery and mark it as 'alive'.
            KMessage.PingResponse pingResponse = (KMessage.PingResponse) message;
            String t = pingResponse.getT();
            String nodeId = (String) pingResponse.getR(KMessage.KMESSAGE_KEY_ID);
            PingTask ping = pingTasks.get(nodeId);
            // transaction id equals -> double check.
            if (ping != null && ping.getTransactionId().equals(t)) {
                ping.setResponseReceived(true);
            }
        } else if(message instanceof KMessage.FindNodeQuery) {
            KMessage.FindNodeQuery findNodeQuery = (KMessage.FindNodeQuery)message;
            String targetNodeId = findNodeQuery.getA(KMessage.KMESSAGE_QUERY_KEY_TARGET);
            // if there is only one node, maybe, it's the node to be found.
            List<Node> closestNodes = this.routingTable.getClosest8Nodes(targetNodeId);
            String nodesString = Utils.createCompactNodesString(closestNodes);
            // TODO: the second argument is correct?
            // nodeString is correct?
            KMessage.FindNodeResponse findNodeResponse = new KMessage.FindNodeResponse(findNodeQuery.getT(), DHTClient.selfNodeId, nodesString);

            Datagram datagram = new Datagram(findNodeQuery.getRemoteAddress(), findNodeResponse.bencode());
            try {
                udpServer.addDatagramToSend(datagram);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else if(message instanceof KMessage.FindNodeResponse) {
            KMessage.FindNodeResponse findNodeResponse = (KMessage.FindNodeResponse)message;
            String nodes = (String)findNodeResponse.getR(KMessage.KMESSAGE_RESPONSE_KEY_NODES);

            FindNodeTask findNodeTask = this.findNodeTasks.get(findNodeResponse.getT());

            for(Node node : Utils.parseCompactNodes(nodes)) {
                // TODO(NOTICE)! node must have nodeId, and address.

                findNodeTask.putQueryingNode(node);
                // if have found the target node, do nothing and put it in the querying queue,
                // the FindNodeThread will check the nodes in the queringNodes queue.
            }
        }else if(message instanceof KMessage.GetPeersQuery) {
            KMessage.GetPeersQuery getPeersQuery = (KMessage.GetPeersQuery)message;
            String infohash = getPeersQuery.getA(KMessage.KMESSAGE_QUERY_KEY_INFO_HASH);
            List<Node> peers = infohashs.get(infohash);

            KMessage.GetPeersResponse getPeersResponse;

            if(peers.size() > 0) {
                List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
                for (Node peer : peers) {
                    addresses.add(peer.getAddress());
                }
                // TODO: the second parameter is correct? is it the node id of the get_peers requester?
                // TODO: generate a token?
                getPeersResponse = new KMessage.GetPeersResponse(getPeersQuery.getT(), getPeersQuery.getA(KMessage.KMESSAGE_KEY_ID), "aoeusnth", Utils.createCompactPeerStringList(addresses));
            }else{
                List<Node> foundNodes = this.routingTable.getClosest8Nodes(infohash);
                String nodesString = Utils.createCompactNodesString(foundNodes);
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
            String token = (String)getPeersResponse.getR(KMessage.KMESSAGE_QUERY_KEY_TOKEN);

            GetPeersResponsedNode getPeersResponsedNode = new GetPeersResponsedNode(getPeersResponse.getRemoteAddress(), token);
            GetPeersTask getPeersTask = this.getPeersTasks.get(getPeersResponse.getT());

            getPeersTask.putResponsedNode(getPeersResponsedNode);

            String nodes = (String)getPeersResponse.getR(KMessage.KMESSAGE_RESPONSE_KEY_NODES);

            if(nodes != null) {
                for (Node node : Utils.parseCompactNodes(nodes)) {
                    getPeersTask.putQueryingNode(node);
                }
            }else{
                List<String> peersList = (List<String>)getPeersResponse.getR(KMessage.KMESSAGE_RESPONSE_KEY_VALUES);
                List<InetSocketAddress> peers = Utils.parseCompactPeers(peersList);
                for(InetSocketAddress peer : peers) {
                    getPeersTask.putPeer(peer);
                }
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
            Node node  = new Node(nodeId, new InetSocketAddress(announcePeerQuery.getRemoteAddress().getAddress(), port));

            List<Node> nodes = this.infohashs.get(infohash);
            if(nodes == null)
                nodes = new ArrayList<Node>();
            nodes.add(node);
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
}
