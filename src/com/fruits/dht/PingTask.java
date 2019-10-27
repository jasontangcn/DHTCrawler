package com.fruits.dht;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PingTask {
    // if
    private final int PING_TIME_OUT = 60 * 1000; // ms
    private final long endTime;

    private final String transactionId;
    // nodeId of requester(myself)
    private final String nodeId;

    private final KMessage.PingQuery pingQuery;
    private ByteBuffer pingQueryBytes;

    private boolean responseReceived;

    public PingTask(String transactionId, String nodeId) {
        this.transactionId = transactionId;
        this.nodeId = nodeId;
        // TODO: may be, we should set the timeout when UDP server has sent out this ping query.
        this.endTime = System.currentTimeMillis() + PING_TIME_OUT;
        this.pingQuery = new KMessage.PingQuery(transactionId, nodeId);
        try {
            this.pingQueryBytes = this.pingQuery.bencode();
        }catch(IOException e){ // technically bencode will never throw the exception.
            e.printStackTrace();
        }
    }

    public boolean isTimeout() {
        return (System.currentTimeMillis() > this.endTime);
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public ByteBuffer getPingQueryBytes() {
        return this.pingQueryBytes;
    }

    // if response received before timout, responseReceived will be set to true;
    public boolean isResponseReceived() {
        return this.responseReceived;
    }

    public void setResponseReceived(boolean responseReceived) {
        this.responseReceived = responseReceived;
    }
}
