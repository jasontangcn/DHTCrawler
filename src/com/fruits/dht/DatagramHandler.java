package com.fruits.dht;

import com.fruits.dht.krpc.KMessage;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ArrayBlockingQueue;

public class DatagramHandler {
    private DHTClient client;
    private final DatagramChannel serverChannel;

    private ArrayBlockingQueue<Datagram> datagramsToSend = new ArrayBlockingQueue<Datagram>(1024, true);

    public DatagramHandler(DHTClient client, DatagramChannel serverChannel) {
        this.client = client;
        this.serverChannel = serverChannel;
    }

    public void readDatagram() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        serverChannel.receive(buffer);
        KMessage message = KMessage.parseMessage(buffer);
        client.handleMessage(message);
    }

    public void sendDatagram() throws IOException {
        Datagram datagram = this.datagramsToSend.poll();
        if (datagram != null) {
            serverChannel.send(datagram.getData(), datagram.getAddress());
        }
    }
}
