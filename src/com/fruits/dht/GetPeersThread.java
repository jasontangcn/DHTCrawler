package com.fruits.dht;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class GetPeersThread implements Runnable {
    private final int GET_PEERS_THREAD_TIME_OUT = 5 * 60 * 1000; // ms

    private final GetPeersTask geetPeersTask;
    private final DHTManager dhtManager;

    public GetPeersThread(GetPeersTask geetPeersTask, DHTManager dhtManager) {
        this.geetPeersTask = geetPeersTask;
        this.dhtManager = dhtManager;
    }

    public void run() {
        long endTime = System.currentTimeMillis() + GET_PEERS_THREAD_TIME_OUT;
        String transactionId = geetPeersTask.getTransactionId();
        String infohash = geetPeersTask.getInfohash();

        for(;;) {
            if(Thread.interrupted())
                break;

            if(System.currentTimeMillis() > endTime)
                break;

            try {
                // firstly check the peers list,
                // if it is not empty, we have found some peers who have the infohash so we could stop the querying.
                // if it is empty,
                LinkedBlockingQueue<InetSocketAddress> peers = geetPeersTask.getPeers();
                if(!peers.isEmpty()) {
                    // TODO: complete the logic here.
                    dhtManager.getQueries().remove(transactionId);
                    dhtManager.removeGetPeersTask(transactionId);
                    break;
                }

                // waiting for 5 ms if there is no object.
                // so we could check whether it is timeout.
                Node node = geetPeersTask.getQueryingNodes().poll(5, TimeUnit.MILLISECONDS);

                if(node != null) {
                    String nodeId = node.getId();

                    // put will block till there is space.
                    geetPeersTask.getQueriedNodes().put(node);

                    // emit another findNode request.
                    dhtManager.putQuery(transactionId, geetPeersTask.getGetPeersQuery());
                    ByteBuffer bytes = geetPeersTask.getGetPeersQueryBytes();
                    bytes.rewind();
                    Datagram datagram = new Datagram(node.getAddress(), bytes);
                    dhtManager.getUdpServer().addDatagramToSend(datagram);
                }
            }catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
