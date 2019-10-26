package com.fruits.dht;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

public class UDPServer {
    private DHTManager dhtManager;

    private Selector selector;
    private DatagramChannel serverChannel;

    private ArrayBlockingQueue<Datagram> datagramsToSend = new ArrayBlockingQueue<Datagram>(1024, true);

    public UDPServer(DHTManager dhtManager) throws IOException {
        this.dhtManager = dhtManager;
        this.selector = Selector.open();
    }

    public void run() throws IOException {
        this.serverChannel = DatagramChannel.open();
        serverChannel.configureBlocking(false);
        //channel.setOption()
        serverChannel.socket().bind(new InetSocketAddress(DHTClient.LISTENER_DOMAIN, DHTClient.LISTENER_PORT));
        serverChannel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        for (; ; ) {
            if (Thread.interrupted())
                break;
            selector.select();
            Iterator<SelectionKey> selectionKeys = selector.selectedKeys().iterator();
            while (selectionKeys.hasNext()) {
                SelectionKey key = (SelectionKey) selectionKeys.next();
                selectionKeys.remove();

                if (!key.isValid())
                    continue;

                if (key.isReadable()) {
                    readDatagram();
                }
                if (key.isWritable()) {
                    sendDatagram();
                }
            }
        }
    }

    public void readDatagram() throws IOException {
        // per the document, udp packet should be <= 512 to avoid fragmentation.
        ByteBuffer buffer = ByteBuffer.allocate(512);
        InetSocketAddress remoteAddress = (InetSocketAddress)serverChannel.receive(buffer);
        // queries used to check the corresponding request by transaction id,
        // so we could know what kind of KMessage this message is.
        KMessage message = KMessage.parseKMessage(remoteAddress, buffer, dhtManager.getQueries());
        dhtManager.handleMessage(message);
    }

    public void sendDatagram() throws IOException {
        Datagram datagram = this.datagramsToSend.poll();
        if (datagram != null) {
            serverChannel.send(datagram.getData(), datagram.getAddress());
        }
    }

    public DatagramChannel getServerChannel() {
        return this.serverChannel;
    }

    public void addDatagramToSend(Datagram datagram) throws InterruptedException {
        this.datagramsToSend.put(datagram);
    }
}
