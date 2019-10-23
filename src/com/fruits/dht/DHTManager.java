package com.fruits.dht;

import com.fruits.dht.krpc.KMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DHTManager {
    private final UDPServer udpServer;

    // transaction id -> Query
    public final Map<String, KMessage.Query> queries = new HashMap<String, KMessage.Query>();

    private Map<String, FindNodeTask> findNodeTasks = new HashMap<String, FindNodeTask>();

    public DHTManager() throws IOException {
        this.udpServer = new UDPServer(this);
    }

    public void findNode(String targetNodeId) {
        String transactionId = Utils.generateTransactionId();
        FindNodeTask findNodeTask = new FindNodeTask(transactionId, targetNodeId);
        FindNodeThread findNodeThread = new FindNodeThread(findNodeTask, this);
        new Thread(findNodeThread).start();
    }

    public void handleMessage(KMessage message) {

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
}
