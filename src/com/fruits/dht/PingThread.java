package com.fruits.dht;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PingThread implements Runnable {
    private final int PING_THREAD_INTERVAL = 5 * 1000; // ms

    private final DHTManager dhtManager;
    private final RoutingTable routingTable;


    public PingThread(DHTManager dhtManager, RoutingTable routingTable) {
        this.dhtManager = dhtManager;
        this.routingTable = routingTable;
    }

    // PingThread always is running till system stops working.
    public void run() {
        for(;;) {
            // so system could stop this thread.
            if(Thread.interrupted())
                break;

            List<Node> nodes = routingTable.getNodes();

            for(Node node : nodes) {
                // TODO: should we grantee that any node in routing table have a node id?
                String nodeId = node.getId();

                PingTask pingTask = dhtManager.pingTasks.get(nodeId);

                // a ping to the node is in progress.
                if(pingTask != null) {
                    if(pingTask.isResponseReceived()) {
                        dhtManager.pingTasks.remove(nodeId);
                    }else if (pingTask.isTimeout()) {
                        dhtManager.pingTasks.remove(nodeId);

                        // the node is BAD, remove it from  routing table.
                        routingTable.removeNode(node);
                        routingTable.removeNodeFromBucket(node);
                    }
                }else{
                    // create a ping to the node
                    PingTask ping = new PingTask(Utils.generateTransactionId(), nodeId);
                    dhtManager.pingTasks.put(nodeId, ping);

                    dhtManager.putQuery(ping.getTransactionId(), ping.getPingQuery());

                    ByteBuffer bytes = ping.getPingQueryBytes();
                    bytes.rewind();
                    Datagram datagram = new Datagram(node.getAddress(), bytes);
                    try {
                        dhtManager.getUdpServer().addDatagramToSend(datagram);
                    }catch(InterruptedException e) {
                        e.printStackTrace();;
                    }
                }
            }

            try {
                Thread.sleep(PING_THREAD_INTERVAL);
            }catch(InterruptedException e) {
                e.printStackTrace();;
            }
        }
    }
}
