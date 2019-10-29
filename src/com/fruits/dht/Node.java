package com.fruits.dht;

import java.net.InetSocketAddress;
import java.util.Objects;

public class Node {
    // TODO(NOTICE)! this field is required!
    protected String id; // NodeId
    protected int bucketIndex;

    // SocketAddress -> hostname, ip but no port.
    protected InetSocketAddress address; // hostname + port

    public enum NodeStatus {
        UNKNOWN(-1), BAD(0), DUBIOUS(1), GOOD(2);

        private int statusId;

        NodeStatus(int statusId) {
            this.statusId = statusId;
        }
    }

    protected NodeStatus status = NodeStatus.GOOD; // good, bad or dubious

    public Node() {
    }

    public Node(String id, InetSocketAddress address) {
        this.id = id;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getBucketIndex() {
        return bucketIndex;
    }

    public void setBucketIndex(int bucketIndex) {
        this.bucketIndex = bucketIndex;
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

    @Override
    public String toString() {
        return "Node{" +
            "id='" + id + '\'' +
            ", bucketIndex=" + bucketIndex +
            ", address=" + address +
            ", status=" + status +
            '}';
    }

    // TODO: should equals involves "address"?
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id);
    }

    // TODO: rewrite hashCode.
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
