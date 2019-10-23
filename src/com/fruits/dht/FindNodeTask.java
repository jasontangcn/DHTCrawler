package com.fruits.dht;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class FindNodeTask {
    private final String transactionId;
    private final String targetNodeId;

    private PriorityBlockingQueue<Node> queryingNodes = new PriorityBlockingQueue<Node>();
    private LinkedBlockingQueue<Node> queriedNodes = new LinkedBlockingQueue<Node>(); //
    private LinkedBlockingQueue<Node> responsedNodes = new LinkedBlockingQueue<Node>(); // used for announce_peer

    public FindNodeTask(String transactionId, String targetNodeId) {
        this.transactionId = transactionId;
        this.targetNodeId = targetNodeId;
    }

    public PriorityBlockingQueue<Node> getQueryingNodes() {
        return queryingNodes;
    }

    public LinkedBlockingQueue<Node> getQueriedNodes() {
        return queriedNodes;
    }

    public LinkedBlockingQueue<Node> getResponsedNodes() {
        return responsedNodes;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }
}
