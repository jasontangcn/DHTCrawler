package com.fruits.dht;

import com.fruits.dht.krpc.KMessage;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import static com.fruits.dht.krpc.KMessage.*;

public class DHTClient {
    private String nodeId;
    private RoutingTable routingTable = new RoutingTable();

    public DHTClient() throws NoSuchAlgorithmException {
        this.nodeId = Utils.generateNodeId();
    }

    public void start() throws IOException, UnsupportedEncodingException {
        // 1. build a routing table.
        // 2. initialize the routing table by finding node "67d0515bcf1e9ddb25ca909135c2c684b41f1dbe",
        //    router.bittorrent.com:6881、 dht.transmissionbt.com:6881
        // 3.
        String transactionId = Utils.generateTransactionId();
        ByteBuffer findNodeRequest = createFindNodeRequest(transactionId, nodeId, "e5591e20a8f02398a9948c4e35ccfc6b3da21a56");
        Datagram datagram = new Datagram(new InetSocketAddress("dht.transmissionbt.com", 6881), findNodeRequest);
    }

    public void handleMessage(KMessage message) {

    }

    public static void main(String[] args) {
    }
}
