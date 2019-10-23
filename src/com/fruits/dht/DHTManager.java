package com.fruits.dht;

import com.fruits.dht.krpc.KMessage;

import java.util.HashMap;
import java.util.Map;

public class DHTManager {
    // transaction id -> Query
    public static final Map<String, KMessage.Query> queries = new HashMap<String, KMessage.Query>();

    private Map<String, FindNodeTask> findNodeTasks = new HashMap<String, FindNodeTask>();
}
