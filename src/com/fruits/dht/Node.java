package com.fruits.dht;

import java.net.SocketAddress;

public class Node {
    private SocketAddress address;
    private int port;
    private String id;
    private char status; // good, bad or dubious
}
