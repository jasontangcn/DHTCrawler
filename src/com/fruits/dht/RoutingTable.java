package com.fruits.dht;

import java.util.ArrayList;
import java.util.List;

public class RoutingTable {
    public static final int BULK_MIN_INDEX = 0;
    public static final int BULK_MAX_INDEX = 160; // exclusive

    private List<Node> nodes = new ArrayList<Node>();

    public void addNodes(List<Node> nodes) {

    }

    public List<Node> getNodes() {
        return this.nodes;
    }

    public void removeNode(Node node) {
        this.nodes.remove(node);
    }

    private Bulk head = new Bulk(BULK_MIN_INDEX, BULK_MAX_INDEX);

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

    }
}
