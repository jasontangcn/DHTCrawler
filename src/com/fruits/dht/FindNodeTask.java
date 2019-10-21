package com.fruits.dht;

import java.util.ArrayList;
import java.util.List;

public class FindNodeTask {
    private List<Node> queryingNodes = new ArrayList<Node>();
    private List<Node> queriedNodes = new ArrayList<Node>(); //
    private List<Node> responsedNodes = new ArrayList<Node>(); // used for announce_peer

    public FindNodeTask() {
    }

    public List<Node> getQueryingNodes() {
        return queryingNodes;
    }

    public List<Node> getQueriedNodes() {
        return queriedNodes;
    }

    public List<Node> getResponsedNodes() {
        return responsedNodes;
    }
}
