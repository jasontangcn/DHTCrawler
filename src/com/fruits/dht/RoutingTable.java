package com.fruits.dht;

import java.util.*;

/*
    Logic of RoutingTable:
    1. Any request or response could be used to build the routing table?
       When I get a request, usually I know the node id of the sender, and the endpoint at which the request is sent,
       but how could I know the endpoint sender is listening at?
       Just consider the endpoint for receiving same with the endpoint for sending?

    2. announce_peer is used to notify the nodes who have responded my requests and
       the nodes closest to the infohash that I have got the peer list.

    3.
 */

public class RoutingTable {
    public static final int BUCKET_MIN_INDEX = 0;
    public static final int BUCKET_MAX_INDEX = 160; // exclusive

    // initial bucket.
    private Bucket head = new Bucket(BUCKET_MIN_INDEX, BUCKET_MAX_INDEX, this);
    private List<Node> nodes/*the same nodes in buckets*/ = new ArrayList<Node>();

    private DHTManager dhtManager;

    public RoutingTable(DHTManager dhtManager) {
        this.dhtManager = dhtManager;

        PingThread pingThread = new PingThread(dhtManager, this);
        new Thread(pingThread).start();
    }

    // TODO: PingThread may call this method
    public List<Node> getNodes() {
        return this.nodes;
    }

    // TODO: only Bucket could call this method.
    public void putNode(Node node) {
        this.nodes.add(node);
    }

    // TODO:
    public boolean putNodeInBucket(Node node) {
        // TODO: node must have a node id.
        String nodeId = node.getId();
        if(nodeId == null)
            return false;

        // nodeId is a 160 bits string
        // bucketIndex range: 0 - 160(exclusive)
        // [2^0, 2^1)
        // [2^1, 2^2)
        // ...
        // [2^159, 2^160)

        // firstly there is only one bucket with bucketIndex range [2^0, 2^160)

        int index = Utils.getLog2(Utils.hexStringToBytes(nodeId));
        node.setBucketIndex(index);

        Bucket bucket = head;

        while(index >= bucket.getMaxIndex()) {
            bucket = bucket.getNext();
            if(bucket == null)
                break;
        }

        if(bucket != null) {
            bucket.putNode(node);
            return true;
        }

        return false;
    }

    // TODO:
    // nodeId could a node id or a infohash of a seed?

    // K == 8
    // this api works for find_node request.
    // if target node is found, return it otherwise return 8 closest nodes
    // TODO: important function, need to be very careful about the details.
    public List<Node> getClosest8Nodes(String nodeId/* nodeId could be a infohash value*/) {
        List<Node> foundNodes = new ArrayList<Node>();

        if(nodes.size() > 0) {
            for (Node node : nodes) {
                if (node.getId().equals(nodeId)) {
                    foundNodes.add(node);
                    // only one node will be returned, we have found the exact node.
                    return foundNodes;
                }
            }

            // sort the nodes by distance from 'node' then return the closest 8 nodes
            Node[] nodesArray = (Node[]) nodes.toArray(new Node[0]);

            Arrays.sort(nodesArray, new NodeComparator(nodeId));

            for (int i = 0; i < nodesArray.length && i < Bucket.BUCKET_CAPACITY_MAX; i++) {
                foundNodes.add(nodesArray[i]);
            }
        }
        return foundNodes;
    }

    // 1. remove node from nodes,
    // 2. remove node from buckets.
    public void removeNodeFromRoutingTable(Node node) {
        this.nodes.remove(node);

        Bucket bucket = head;
        do {
            boolean removed = bucket.removeNode(node);
            if(removed)
                break;
            bucket = bucket.getNext();
        }while(bucket != null);
    }
}
