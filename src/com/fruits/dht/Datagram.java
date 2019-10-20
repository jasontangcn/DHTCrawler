package com.fruits.dht;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class Datagram {
    private SocketAddress address;
    private ByteBuffer data;

    public Datagram(SocketAddress address, ByteBuffer data) {
        this.address = address;
        this.data = data;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public void setAddress(SocketAddress address) {
        this.address = address;
    }

    public ByteBuffer getData() {
        return data;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }
}
