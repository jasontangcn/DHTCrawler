package com.fruits.dht;

import java.util.*;

public class RoutingTable {
    public static final int BUCKET_MIN_INDEX = 0;
    public static final int BUCKET_MAX_INDEX = 160; // exclusive

    private Bucket head = new Bucket(BUCKET_MIN_INDEX, BUCKET_MAX_INDEX);
    public static List<Node> nodes = new ArrayList<Node>();


    public List<Node> getNodes() {
        return this.nodes;
    }

    public void removeNode(Node node) {
        this.nodes.remove(node);
    }

    public void addNode(Node node) {
        String nodeId = node.getId();
        if(nodeId == null)
            return;

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
            bucket.addNode(node);
        }
    }

    // K == 8
    // this api works for find_node request.
    // if target node is found, return it otherwise return 8 closest nodes
    // TODO: important function, need to be very careful about the details.
    public List<Node> getClosest8Nodes(String nodeId) {
        List<Node> expectedNodes = new ArrayList<Node>();

        for(Node node : nodes) {
            if(node.getId().equals(nodeId)) {
                expectedNodes.add(node);
                return expectedNodes;
            }
        }

        // sort the nodes by distance from 'node' then return the closest 8 nodes

        Node[] nodesArray = (Node[])nodes.toArray();

        Arrays.sort(nodesArray, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                // TODO: special handling, e.g. null assert,
                Bitmap distance1 = new Bitmap(Utils.getDistance(((Node)o1).getId(), nodeId));
                Bitmap distance2 = new Bitmap(Utils.getDistance(((Node)o2).getId(), nodeId));
                int length = distance1.length();

                for(int i = 0; i < length; i++) {
                    if(distance1.get(i) && !distance2.get(i)) {
                        return 1;
                    }else if(!distance1.get(i) && distance2.get(i)) {
                        return -1;
                    }
                }

                return 0;
            }
        });

        for(int i = 0; i < nodesArray.length && i < Bucket.BUCKET_CAPACITY_MAX; i++) {
            expectedNodes.add(nodesArray[i]);
        }

        return expectedNodes;
    }
}
