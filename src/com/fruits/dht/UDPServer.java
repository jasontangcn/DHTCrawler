package com.fruits.dht;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public class UDPServer {
    private DHTClient client;

    private Selector selector;
    private DatagramChannel serverChannel;

    private DatagramHandler datagramHandler;

    public UDPServer(DHTClient client) throws IOException {
        this.client = client;
        this.selector = Selector.open();
    }

    public void run() throws IOException {
        this.serverChannel = DatagramChannel.open();
        datagramHandler = new DatagramHandler(this.client, this.serverChannel);
        serverChannel.configureBlocking(false);
        //channel.setOption()
        serverChannel.socket().bind(new InetSocketAddress("10.129.10.100", 6666));
        serverChannel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        for(;;){
            if(Thread.interrupted())
                break;
            selector.select();
            Iterator<SelectionKey> selectionKeys = selector.selectedKeys().iterator();
            while(selectionKeys.hasNext()) {
                SelectionKey key = (SelectionKey)selectionKeys.next();
                selectionKeys.remove();

                if(!key.isValid())
                    continue;

                if(key.isReadable()){
                    this.datagramHandler.readDatagram();
                }if(key.isWritable()){
                    this.datagramHandler.sendDatagram();
                }
            }
        }
    }
}
