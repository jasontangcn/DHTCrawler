package com.fruits.dht;

import java.util.concurrent.ArrayBlockingQueue;

public class Bucket {
    public static final int BUCKET_CAPACITY_MAX = 8;

    private int minIndex; // inclusive
    private int maxIndex; // exclusive

    ArrayBlockingQueue<Node> nodes = new ArrayBlockingQueue<>(BUCKET_CAPACITY_MAX); // default capacity is 8.

    private Bucket next;

    public Bucket(int minIndex, int maxIndex) {
        this.minIndex = minIndex;
        this.maxIndex = maxIndex;
    }

    public Bucket getNext() {
        return next;
    }

    public void setNext(Bucket next) {
        this.next = next;
    }

    public int getMinIndex() {
        return minIndex;
    }

    public void setMinIndex(int minIndex) {
        this.minIndex = minIndex;
    }

    public int getMaxIndex() {
        return maxIndex;
    }

    public void setMaxIndex(int maxIndex) {
        this.maxIndex = maxIndex;
    }

    public void addNode(Node node) {
        if(nodes.contains(node)) {
            /**
             * Removes a single instance of the specified element from this queue,
             * if it is present.  More formally, removes an element {@code e} such
             * that {@code o.equals(e)}, if this queue contains one or more such
             * elements.
             * Returns {@code true} if this queue contained the specified element
             * (or equivalently, if this queue changed as a result of the call).
             *
             * <p>Removal of interior elements in circular array based queues
             * is an intrinsically slow and disruptive operation, so should
             * be undertaken only in exceptional circumstances, ideally
             * only when the queue is known not to be accessible by other
             * threads.
             *
             * @param o element to be removed from this queue, if present
             * @return {@code true} if this queue changed as a result of the call
             */
            // TODO: risky?
            nodes.remove(node);
            try{
                nodes.put(node);}
            catch(InterruptedException e) {
                e.printStackTrace();
            }
        }else{
            if(nodes.remainingCapacity() > 0) {
                try{
                    //TODO: put the node in a sorted indexed sequential container,
                    nodes.put(node);
                }catch(InterruptedException e) {
                    e.printStackTrace();;
                }
            }else{
                int selfNodeIndex = Utils.getLog2(Utils.hexStringToBytes(DHTClient.selfNodeId));
                if(selfNodeIndex >= minIndex && selfNodeIndex < maxIndex) {
                    int range = maxIndex - minIndex;
                    if(range > 1) {
                        int half = range/2;

                        int newIndex = (minIndex + half);

                        this.maxIndex = newIndex;

                        Bucket newBucket = new Bucket(newIndex, maxIndex);

                        // relocate the nodes in current bucket
                        for(Node o : nodes) {
                            if(o.getBucketIndex() >= newIndex) {
                                // TODO: put the node in a sorted indexed sequential container,
                                newBucket.addNode(o);
                                nodes.remove(o);
                            }
                        }

                        newBucket.next = this.next;
                        this.next = newBucket;
                    }
                }
            }
        }
    }
}
