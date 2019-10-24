package com.fruits.dht;

import com.fruits.dht.krpc.KMessage;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
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

    private Map<String, FindNodeTask> findNodeTasks = new HashMap<String, FindNodeTask>();

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
        findNodeTasks.put(findNodeTask.getTargetNodeId(), findNodeTask);
        findNodeTask.getQueryingNodes().put(closerNode);
        FindNodeThread findNodeThread = new FindNodeThread(findNodeTask, this);
        new Thread(findNodeThread).start();
    }

    public void handleMessage(KMessage message) throws IOException {
        if(message instanceof KMessage.FindNodeResponse) {
            KMessage.FindNodeResponse findNodeResponse = (KMessage.FindNodeResponse)message;
            String nodes = (String)findNodeResponse.getR(KMessage.KMESSAGE_RESPONSE_KEY_NODES);
            for(Node node : Utils.parseCompactNodes(nodes)) {
                FindNodeTask findNodeTask = this.findNodeTasks.get(findNodeResponse.getT());
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
}
