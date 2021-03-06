package com.fruits.dht;

import java.nio.ByteBuffer;
import java.util.List;

public class PingThread implements Runnable {
    private final int PING_THREAD_INTERVAL = 60 * 1000; // ms

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
                System.out.println("[PingThread] get a node : [" + node + "].");
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
                        routingTable.removeNodeFromRoutingTable(node);
                    }
                }else{
                    // create a ping to the node
                    pingTask = new PingTask(Utils.generateTransactionId(), nodeId);
                    dhtManager.pingTasks.put(nodeId, pingTask);

                    dhtManager.putQuery(pingTask.getTransactionId(), pingTask.getPingQuery());

                    ByteBuffer bytes = pingTask.getPingQueryBytes();
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
