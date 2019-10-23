package com.fruits.dht;

import com.fruits.dht.krpc.KMessage;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class FindNodeThread implements Runnable {
    private final int FIND_NODE_THREAD_TIME_OUT = 5 * 60 * 1000; // ms
    private final FindNodeTask findNodeTask;

    private final DatagramHandler datagramHandler;

    public FindNodeThread(FindNodeTask findNodeTask, DatagramHandler datagramHandler) {
        this.findNodeTask = findNodeTask;
        this.datagramHandler = datagramHandler;
    }

    public void run() {
        long endTime = System.currentTimeMillis() + FIND_NODE_THREAD_TIME_OUT;

        for(;;) {
            if(Thread.interrupted())
                break;

            if(System.currentTimeMillis() > endTime)
                break;

            try {
                // waiting for 5 ms if there is no object.
                // so we could check whether it is timeout.
                Node node = findNodeTask.getQueryingNodes().poll(5, TimeUnit.MILLISECONDS);
                if(node != null) {
                    // put will block till there is space.
                    findNodeTask.getQueriedNodes().put(node);
                }

                // emit another findNode request.
                try {
                    Datagram datagram = new Datagram(node.getAddress(), new KMessage.FindNodeQuery(findNodeTask.getTransactionId(), DHTClient.selfNodeId, findNodeTask.getTargetNodeId()).bencode());
                    datagramHandler.addDatagramToSend(datagram);
                }catch(IOException e){
                    e.printStackTrace();
                }
            }catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
