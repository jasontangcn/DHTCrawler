package com.fruits.dht;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class GetPeersThread implements Runnable {
    private final int GET_PEERS_THREAD_TIME_OUT = 5 * 60 * 1000; // ms

    private final GetPeersTask getPeersTask;
    private final DHTManager dhtManager;

    public GetPeersThread(GetPeersTask getPeersTask, DHTManager dhtManager) {
        this.getPeersTask = getPeersTask;
        this.dhtManager = dhtManager;
    }

    public void run() {
        long endTime = System.currentTimeMillis() + GET_PEERS_THREAD_TIME_OUT;
        String transactionId = getPeersTask.getTransactionId();
        String infohash = getPeersTask.getInfohash();

        for(;;) {
            if(Thread.interrupted())
                break;

            if(System.currentTimeMillis() > endTime)
                break;

            try {
                // firstly check the peers list,
                // if it is not empty, we have found some peers who have the infohash so we could stop the querying.
                // if it is empty,
                //LinkedBlockingQueue<InetSocketAddress> peers = getPeersTask.getPeers();

                if(getPeersTask.isFoundSomePeers()) {
                    // TODO: complete the logic here.
                    dhtManager.removeQuery(transactionId);
                    dhtManager.removeGetPeersTask(transactionId);
                    break;
                }

                // waiting for 5 ms if there is no object.
                // so we could check whether it is timeout.
                Node node = getPeersTask.getQueryingNodes().poll(5, TimeUnit.MILLISECONDS);

                if(node != null) {
                    // TODO(NOTICE)! if nodeId == null, it may be starting node
                    String nodeId = node.getId();

                    // put will block till there is space.
                    if(node.getId() != null)
                        getPeersTask.getQueriedNodes().put(node);

                    // emit another findNode request.
                    if(dhtManager.getQuery(transactionId) == null)
                        dhtManager.putQuery(transactionId, getPeersTask.getGetPeersQuery());

                    ByteBuffer bytes = getPeersTask.getGetPeersQueryBytes();
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
