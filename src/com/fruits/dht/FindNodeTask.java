package com.fruits.dht;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class FindNodeTask {
    // all of the requests for one find_node query use only one transaction id.
    private final String transactionId;
    private final String targetNodeId;

    private final KMessage.FindNodeQuery findNodeQuery;
    private final ByteBuffer findNodeQueryBytes;

    // TODO: have not provided a comparator!
    private PriorityBlockingQueue<Node> queryingNodes;
    private LinkedBlockingQueue<Node> queriedNodes = new LinkedBlockingQueue<Node>(); //

    public FindNodeTask(String transactionId, String targetNodeId) throws IOException {
        this.transactionId = transactionId;
        this.targetNodeId = targetNodeId;

        queryingNodes = new PriorityBlockingQueue<Node>(160, new NodeComparator(targetNodeId));

        this.findNodeQuery = new KMessage.FindNodeQuery(transactionId, DHTManager.selfNodeId, targetNodeId);
        this.findNodeQueryBytes = findNodeQuery.bencode();
    }

    public PriorityBlockingQueue<Node> getQueryingNodes() {
        return this.queryingNodes;
    }

    public LinkedBlockingQueue<Node> getQueriedNodes() {
        return this.queriedNodes;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public String getTargetNodeId() {
        return this.targetNodeId;
    }

    public KMessage.FindNodeQuery getFindNodeQuery() {
        return this.findNodeQuery;
    }

    public ByteBuffer getFindNodeQueryBytes() {
        return this.findNodeQueryBytes;
    }

    public boolean putQueryingNode(Node node) {
        if(queriedNodes.contains(node) || queryingNodes.contains(node))
            return false;
        queryingNodes.put(node);
        return true;
    }
}
