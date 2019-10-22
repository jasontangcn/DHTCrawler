package com.fruits.dht;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class Datagram {
    private InetSocketAddress address; // hostname + port
    private ByteBuffer data; //

    public Datagram(InetSocketAddress address, ByteBuffer data) {
        this.address = address;
        this.data = data;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    public ByteBuffer getData() {
        return data;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }
}
