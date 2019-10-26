package com.fruits.dht;

import java.net.InetSocketAddress;

public class GetPeersResponsedNode extends Node {
    protected String token;

    public GetPeersResponsedNode(Node node, String token) {
        this.id = node.getId();
        this.address = node.getAddress();
        this.token = token;
    }

    public GetPeersResponsedNode(InetSocketAddress address, String token) {
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
