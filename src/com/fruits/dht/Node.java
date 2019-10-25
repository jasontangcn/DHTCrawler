package com.fruits.dht;

import java.net.InetSocketAddress;

public class Node {
    protected String id; // NodeId

    // SocketAddress -> hostname, ip but no port.
    protected InetSocketAddress address; // hostname + port
    protected NodeStatus status = NodeStatus.GOOD; // good, bad or dubious

    public Node() {
    }

    public Node(String id, InetSocketAddress address) {
        this.id = id;
        this.address = address;
    }

    public enum NodeStatus {
        UNKNOWN(-1), BAD(0), DUBIOUS(1), GOOD(2);

        private int statusId;

        NodeStatus(int statusId) {
            this.statusId = statusId;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }
}
