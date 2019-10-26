package com.fruits.dht;

import java.util.*;

public class RoutingTable {
    public static final int BULK_MIN_INDEX = 0;
    public static final int BULK_MAX_INDEX = 160; // exclusive

    private Bulk head = new Bulk(BULK_MIN_INDEX, BULK_MAX_INDEX);
    public static List nodes = new ArrayList<Node>();


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
        // index range: 0 - 160(exclusive)
        // [2^0, 2^1)
        // [2^1, 2^2)
        // ...
        // [2^159, 2^160)

        // firstly there is only one bulk with index range [2^0, 2^160)

        int index = Utils.getLog2(Utils.hexStringToBytes(nodeId));
        node.setIndex(index);

        Bulk bulk = head;

        while(index >= bulk.getMaxIndex()) {
            bulk = bulk.getNext();
            if(bulk == null)
                break;
        }

        if(bulk != null) {
            bulk.addNode(node);
        }
    }

    // K == 8
    // this api works for find_node request.
    // if target node is found, return it otherwise return 8 closest nodes
    public List<Node> getClosest8Nodes(Node node) {
        List<Node> expectedNodes = new ArrayList<Node>();

        if(nodes.contains(node)) {
            expectedNodes.add(node);
            return expectedNodes;
        }

        // sort the nodes by distance from 'node' then return the closest 8 nodes

        Node[] nodesArray = (Node[])nodes.toArray();

        Arrays.sort(nodesArray, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Bitmap distance1 = new Bitmap(Utils.getDistance((Node)o1, node));
                Bitmap distance2 = new Bitmap(Utils.getDistance((Node)o2, node));
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

        for(int i = 0; i < nodesArray.length && i < Bulk.BULK_CAPACITY_MAX; i++) {
            expectedNodes.add(nodesArray[i]);
        }

        return expectedNodes;
    }
}
