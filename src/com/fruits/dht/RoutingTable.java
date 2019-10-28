package com.fruits.dht;

import java.util.*;

public class RoutingTable {
    public static final int BUCKET_MIN_INDEX = 0;
    public static final int BUCKET_MAX_INDEX = 160; // exclusive

    // initial bucket.
    private Bucket head = new Bucket(BUCKET_MIN_INDEX, BUCKET_MAX_INDEX, this);
    public static List<Node> nodes = new ArrayList<Node>();

    // TODO: PingThread may call this method
    public List<Node> getNodes() {
        return this.nodes;
    }

    // TODO: PingThread may call this method.
    public void removeNode(Node node) {
        this.nodes.remove(node);
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

            Arrays.sort(nodesArray, new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    // TODO: special handling, e.g. null assert,
                    Bitmap distance1 = new Bitmap(Utils.getDistance(((Node) o1).getId(), nodeId));
                    Bitmap distance2 = new Bitmap(Utils.getDistance(((Node) o2).getId(), nodeId));
                    int size = distance1.size();

                    for (int i = 0; i < size; i++) {
                        if (distance1.get(i) && !distance2.get(i)) {
                            return 1;
                        } else if (!distance1.get(i) && distance2.get(i)) {
                            return -1;
                        }
                    }

                    return 0;
                }
            });

            for (int i = 0; i < nodesArray.length && i < Bucket.BUCKET_CAPACITY_MAX; i++) {
                foundNodes.add(nodesArray[i]);
            }
        }
        return foundNodes;
    }
}
