package com.fruits.dht;

public class DatagramHandler {
    /*
    private DHTManager dhtManager;
    private final DatagramChannel serverChannel;

    private ArrayBlockingQueue<Datagram> datagramsToSend = new ArrayBlockingQueue<Datagram>(1024, true);

    public DatagramHandler(DHTManager dhtManager, DatagramChannel serverChannel) {
        this.dhtManager = dhtManager;
        this.serverChannel = serverChannel;
    }

    public void readDatagram() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        SocketAddress remoteAddress = serverChannel.receive(buffer);
        KMessage message = KMessage.parseMessage(buffer);
        dhtManager.handleMessage(message);
    }

    public void sendDatagram() throws IOException {
        Datagram datagram = this.datagramsToSend.poll();
        if (datagram != null) {
            serverChannel.send(datagram.getData(), datagram.getAddress());
        }
    }

    public void addDatagramToSend(Datagram datagram) throws InterruptedException {
        this.datagramsToSend.put(datagram);
    }
    */
}
