package com.fruits.dht;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class FindNodeThread implements Runnable {
    private final int FIND_NODE_THREAD_TIME_OUT = 5 * 60 * 1000; // 5 minutes

    private final FindNodeTask findNodeTask;
    private final DHTManager dhtManager;

    public FindNodeThread(FindNodeTask findNodeTask, DHTManager dhtManager) {
        this.findNodeTask = findNodeTask;
        this.dhtManager = dhtManager;
    }

    public void run() {
        long endTime = System.currentTimeMillis() + FIND_NODE_THREAD_TIME_OUT;
        String transactionId = findNodeTask.getTransactionId();
        String targetNodeId = findNodeTask.getTargetNodeId();

        for(;;) {
            if(Thread.interrupted())
                break;

            if(System.currentTimeMillis() > endTime) {
                System.out.println("[FindNodThread] a find_node query has been timeout.");
                break;
            }

            try {
                // waiting for 5 ms if there is no object.
                // so we could check whether it is timeout.
                Node node = findNodeTask.getQueryingNodes().poll(5, TimeUnit.MILLISECONDS);

                if(node != null) {
                    String nodeId = node.getId();

                    // found the target node
                    if(nodeId != null && nodeId.equals(targetNodeId)) {
                        // TODO: how to return the target node?
                        // TODO: how to clear the resource of this find_node request?

                        // finish this FindNodeThread.
                        break;
                    }

                    // put will block till there is space.
                    // TODO(NOTICE): special handling the start node who does not have node id.
                    if(node.getId() != null)
                        findNodeTask.getQueriedNodes().put(node);

                    // emit another findNode request.
                    if(dhtManager.getQuery(transactionId) == null)
                        dhtManager.putQuery(transactionId, findNodeTask.getFindNodeQuery());

                    ByteBuffer bytes = findNodeTask.getFindNodeQueryBytes();
                    bytes.rewind();

                    // TODO(NOTICE):
                    // for the same find_node query, here only different node(address).
                    Datagram datagram = new Datagram(node.getAddress(), bytes);
                    dhtManager.getUdpServer().addDatagramToSend(datagram);
                }
            }catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        dhtManager.removeQuery(transactionId);
        dhtManager.removeFindNodeTask(transactionId);
    }
}
