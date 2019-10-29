package com.fruits.dht;

import java.util.Comparator;

public class NodeComparator implements Comparator<Node> {
    private String targetNodeId;

    public NodeComparator(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public int compare(Node node1, Node node2) {
        Bitmap distance1 = new Bitmap(Utils.getDistance(((Node) node1).getId(), targetNodeId));
        Bitmap distance2 = new Bitmap(Utils.getDistance(((Node) node2).getId(), targetNodeId));
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
}
