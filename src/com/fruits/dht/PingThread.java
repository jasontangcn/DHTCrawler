package com.fruits.dht;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PingThread implements Runnable {
    private final int PING_THREAD_INTERVAL = 5 * 1000;

    private final DHTManager dhtManager;
    private final RoutingTable routingTable;

    // nodeId -> PingTask
    public final static Map<String, PingTask> pingTasks = new HashMap<String, PingTask>();

    public PingThread(DHTManager dhtManager, RoutingTable routingTable) {
        this.dhtManager = dhtManager;
        this.routingTable = routingTable;
    }

    public void run() {
        for(;;) {
            if(Thread.interrupted())
                break;

            List<Node> nodes = routingTable.getNodes();

            for(Node node : nodes) {
                String nodeId = node.getId();
                PingTask pingTask = pingTasks.get(nodeId);
                if(pingTask != null) {
                    if(pingTask.isAlive()) {
                        pingTasks.remove(nodeId);
                    }else if (pingTask.isTimeout()) {
                        pingTasks.remove(nodeId);
                        routingTable.removeNode(node);
                    }
                }else{
                    PingTask ping = new PingTask(Utils.generateTransactionId(), nodeId);
                    pingTasks.put(nodeId, ping);
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
