package com.fruits.dht;

import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public class DHTClient {
    public  static String selfNodeId;

    public static String LISTENER_DOMAIN; // "127.0.0.1"
    public static int LISTENER_PORT; // 6881

    static {
        // Initialize it by JVM options
        Properties props = System.getProperties();

        LISTENER_DOMAIN = props.getProperty("listener.domain");
        LISTENER_PORT = Integer.parseInt(props.getProperty("listener.port"));

        try{
            selfNodeId = Utils.generateNodeId();
        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }
    }

    public DHTClient() {
    }

    public static void main(String[] args) {
    }
}
