package com.fruits.dht;

import java.net.InetSocketAddress;

public class GetPeersReponsedNode extends Node {
    protected String token;

    public GetPeersReponsedNode(Node node, String token) {
        this.id = node.getId();
        this.address = node.getAddress();
        this.token = token;
    }

    public GetPeersReponsedNode(InetSocketAddress address, String token) {
        this.address = address;
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
