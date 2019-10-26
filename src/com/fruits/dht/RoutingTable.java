package com.fruits.dht;

import java.util.ArrayList;
import java.util.List;

public class RoutingTable {
    private List<Node> nodes = new ArrayList<Node>();

    public void addNodes(List<Node> nodes) {

    }

    public void getCloest8Nodes() {

    }

    public List<Node> getNodes() {
        return this.nodes;
    }

    public void removeNode(Node node) {
        this.nodes.remove(node);
    }

    public static class Bulk {

    }
}
