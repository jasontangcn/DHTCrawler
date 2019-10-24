package com.fruits.dht;

import com.fruits.dht.krpc.KMessage;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PingTask {
    private final int PING_TIME_OUT = 60 * 1000;
    private final long endTime;

    private final String transactionId;
    private final String nodeId;

    private final KMessage.PingQuery pingQuery;
    private ByteBuffer pingQueryBytes;

    private boolean alive;

    public PingTask(String transactionId, String nodeId) {
        this.transactionId = transactionId;
        this.nodeId = nodeId;
        this.endTime = System.currentTimeMillis() + PING_TIME_OUT;
        this.pingQuery = new KMessage.PingQuery(transactionId, nodeId);
        try {
            this.pingQueryBytes = this.pingQuery.bencode();
        }catch(IOException e){
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

    public boolean isAlive() {
        return this.alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}
