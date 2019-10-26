package com.fruits.dht;

import com.fruits.dht.krpc.KMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class GetPeersTask {
    // all of the requests for one find_node query use only one transaction id.
    private final String transactionId;
    private final String infohash; // target infohash
    private final KMessage.GetPeersQuery getPeersQuery;
    private final ByteBuffer getPeersQueryBytes;

    // have found some peers who have the infohash
    private LinkedBlockingQueue<InetSocketAddress> peers = new LinkedBlockingQueue<InetSocketAddress>();
    private PriorityBlockingQueue<Node> queryingNodes = new PriorityBlockingQueue<Node>();
    private LinkedBlockingQueue<Node> queriedNodes = new LinkedBlockingQueue<Node>(); //
    private LinkedBlockingQueue<GetPeersResponsedNode> responsedNodes = new LinkedBlockingQueue<GetPeersResponsedNode>(); // used for announce_peer

    public GetPeersTask(String transactionId, String infohash) throws IOException {
        this.transactionId = transactionId;
        this.infohash = infohash;
        this.getPeersQuery = new KMessage.GetPeersQuery(transactionId, DHTClient.selfNodeId, infohash);
        this.getPeersQueryBytes = getPeersQuery.bencode();
    }

    public LinkedBlockingQueue<InetSocketAddress> getPeers() {
        return this.peers;
    }

    public PriorityBlockingQueue<Node> getQueryingNodes() {
        return this.queryingNodes;
    }

    public LinkedBlockingQueue<Node> getQueriedNodes() {
        return this.queriedNodes;
    }

    public LinkedBlockingQueue<GetPeersResponsedNode> getResponsedNodes() {
        return this.responsedNodes;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public String getInfohash() {
        return this.infohash;
    }

    public KMessage.GetPeersQuery getGetPeersQuery() {
        return this.getPeersQuery;
    }

    public ByteBuffer getGetPeersQueryBytes() {
        return this.getPeersQueryBytes;
    }
}
