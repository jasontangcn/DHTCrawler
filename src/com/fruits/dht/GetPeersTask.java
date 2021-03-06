package com.fruits.dht;

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
    //private LinkedBlockingQueue<InetSocketAddress> peers = new LinkedBlockingQueue<InetSocketAddress>();
    private volatile boolean foundSomePeers;

    private PriorityBlockingQueue<Node> queryingNodes = new PriorityBlockingQueue<Node>();
    private LinkedBlockingQueue<Node> queriedNodes = new LinkedBlockingQueue<Node>(); //
    private LinkedBlockingQueue<GetPeersRespondedNode> respondedNodes = new LinkedBlockingQueue<GetPeersRespondedNode>(); // used for announce_peer

    public GetPeersTask(String transactionId, String infohash) throws IOException {
        this.transactionId = transactionId;
        this.infohash = infohash;
        this.getPeersQuery = new KMessage.GetPeersQuery(transactionId, DHTManager.selfNodeId, infohash);
        this.getPeersQueryBytes = getPeersQuery.bencode();
    }

    /*
    public LinkedBlockingQueue<InetSocketAddress> getPeers() {
        return this.peers;
    }
    */

    public PriorityBlockingQueue<Node> getQueryingNodes() {
        return this.queryingNodes;
    }

    public LinkedBlockingQueue<Node> getQueriedNodes() {
        return this.queriedNodes;
    }

    public LinkedBlockingQueue<GetPeersRespondedNode> getRespondedNodes() {
        return this.respondedNodes;
    }

    // TODO: token may timeout, so not sure is it correct to decide whether put the respondedNode in or not?
    public void putRespondedNode(GetPeersRespondedNode node) {
        if(!this.respondedNodes.contains(node)) {
            try{
                this.respondedNodes.put(node);
            }catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
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

    public boolean putQueryingNode(Node node) {
        if(queriedNodes.contains(node) || queryingNodes.contains(node))
            return false;
        queryingNodes.put(node);
        return true;
    }

    /*
    public boolean putPeer(InetSocketAddress peer) {
        if(this.peers.contains(peer))
            return false;
        try {
            this.peers.put(peer);
            return true;
        }catch(InterruptedException e) {
            return false;
        }
    }
    */

    public boolean isFoundSomePeers() {
        return this.foundSomePeers;
    }

    public void setFoundSomePeers(boolean foundSomePeers) {
        this.foundSomePeers = foundSomePeers;
    }
}
