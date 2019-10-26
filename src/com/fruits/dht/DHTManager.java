package com.fruits.dht;

import com.fruits.dht.krpc.KMessage;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
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

    // transactionId -> FindNodeTask
    private Map<String, FindNodeTask> findNodeTasks = new HashMap<String, FindNodeTask>();

    //
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
        FindNodeTask findNodeTask = new FindNodeTask(Utils.generateTransactionId(), targetNodeId);
        findNodeTasks.put(findNodeTask.getTransactionId(), findNodeTask);
        findNodeTask.getQueryingNodes().put(closerNode);
        FindNodeThread findNodeThread = new FindNodeThread(findNodeTask, this);
        new Thread(findNodeThread).start();
    }

    public void getPeers(Node closerNode, String infohash) throws IOException {
        GetPeersTask getPeersTask = new GetPeersTask(Utils.generateTransactionId(), infohash);
        getPeersTasks.put(getPeersTask.getTransactionId(), getPeersTask);
        getPeersTask.getQueryingNodes().put(closerNode);
        GetPeersThread getPeersThread = new GetPeersThread(getPeersTask, this);
        new Thread(getPeersThread).start();
    }

    public void handleMessage(KMessage message) throws IOException {
        if(message instanceof KMessage.PingResponse) {
            KMessage.PingResponse pingResponse = (KMessage.PingResponse)message;
            String nodeId = (String)pingResponse.getR(KMessage.KMESSAGE_KEY_ID);
            Map<String, PingTask> pingTasks = PingThread.pingTasks;
            PingTask ping = pingTasks.get(nodeId);
            if(ping != null) {
                ping.setAlive(true);
            }
        }else if(message instanceof KMessage.FindNodeResponse) {
            KMessage.FindNodeResponse findNodeResponse = (KMessage.FindNodeResponse)message;
            String nodes = (String)findNodeResponse.getR(KMessage.KMESSAGE_RESPONSE_KEY_NODES);

            FindNodeTask findNodeTask = this.findNodeTasks.get(findNodeResponse.getT());

            for(Node node : Utils.parseCompactNodes(nodes)) {
                findNodeTask.getQueryingNodes().put(node);
                // if have found the target node, do nothing and put it in the querying queue,
                // the FindNodeThread will check the nodes in the queringNodes queue.
                /*
                if(node.getId() == findNodeTask.getTransactionId()) {
                    // has found the target node
                }else{
                    findNodeTask.getQueryingNodes().put(node);
                }
                 */
            }
        }else if(message instanceof KMessage.GetPeersResponse) {
            KMessage.GetPeersResponse getPeersResponse = (KMessage.GetPeersResponse)message;
            String token = (String)getPeersResponse.getR(KMessage.KMESSAGE_QUERY_KEY_TOKEN);

            GetPeersReponsedNode getPeersReponsedNode = new GetPeersReponsedNode(getPeersResponse.getResponsedNodeAddress(), token);
            GetPeersTask getPeersTask = this.getPeersTasks.get(getPeersResponse.getT());
            try {
                getPeersTask.getResponsedNodes().put(getPeersReponsedNode);
            }catch(InterruptedException e) {
                e.printStackTrace();
            }

            String nodes = (String)getPeersResponse.getR(KMessage.KMESSAGE_RESPONSE_KEY_NODES);

            if(nodes != null) {
                for (Node node : Utils.parseCompactNodes(nodes)) {
                    getPeersTask.getQueryingNodes().put(node);
                }
            }else{
                List<String> peers = (List<String>)getPeersResponse.getR(KMessage.KMESSAGE_RESPONSE_KEY_VALUES);
                getPeersTask.getPeers().addAll(Utils.parseCompactPeers(peers));
            }
        }
    }

    public UDPServer getUdpServer() {
        return this.udpServer;
    }

    public void putQuery(String transactionId, KMessage.Query query) {
        queries.put(transactionId, query);
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
