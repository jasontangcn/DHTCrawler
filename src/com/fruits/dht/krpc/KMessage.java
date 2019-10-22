package com.fruits.dht.krpc;

import com.turn.torrent.bcodec.BEValue;
import com.turn.torrent.bcodec.BEncoder;

import javax.management.Query;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public abstract class KMessage {
    public static final char KMESSAGE_Y_QUERY = 'q';
    public static final char KMESSAGE_Y_RESPONSE = 'r';
    public static final char KMESSAGE_Y_ERROR = 'e';

    // for y = 'q'
    public static final String KMESSAGE_QUERY_PING = "ping";
    public static final String KMESSAGE_QUERY_FIND_NODE = "find_node";
    public static final String KMESSAGE_QUERY_GET_PEERS = "get_peers";
    public static final String KMESSAGE_QUERY_ANNOUNCE_PEER = "announce_peer";

    protected String v; // optional
    // TODO: tx id contains contrl characters.
    protected String t; // 2 characters(2 bytes) transaction id
    protected char y; // y => q or r or e
    //public String q; -> query
    //public Map a; -> query

    //public String e; -> error

    //public Map r; -> response

    public KMessage(String t) {
        this.t = t;
    }

    public static abstract class QueryMessage extends KMessage {
        protected String q;
        protected Map<String, String> a;

        public QueryMessage(String t, Map a) {
            super(t);
            this.a = a;
            this.y = KMESSAGE_Y_QUERY;
        }

        public String getQ() {
            return q;
        }

        public void setQ(String q) {
            this.q = q;
        }

        public Map getA() {
            return a;
        }

        public void addA(String key, String value) {
            a.put(key, value);
        }
    }

    public static class PingQueryMessage extends QueryMessage {
        public PingQueryMessage(String t, Map<String, String> a) {
            super(t, a);
            this.q = KMESSAGE_QUERY_PING;
        }
    }

    public static class FindNodeQueryMessage extends QueryMessage {
        public FindNodeQueryMessage(String t, Map<String, String> a) {
            super(t, a);
            this.q = KMESSAGE_QUERY_FIND_NODE;
        }
    }

    public static class GetPeersQueryMessage extends QueryMessage {
        public GetPeersQueryMessage(String t, Map<String, String> a) {
            super(t, a);
            this.q = KMESSAGE_QUERY_GET_PEERS;
        }
    }

    public static class AnnouncePeerQueryMessage extends QueryMessage {
        public AnnouncePeerQueryMessage(String t, Map<String, String> a) {
            super(t, a);
            this.q = KMESSAGE_QUERY_ANNOUNCE_PEER;
        }
    }



    public static class ErrorMessage extends KMessage {
        protected String e;

        public ErrorMessage(String t, String e) {
            super(t);
            this.y = KMESSAGE_Y_ERROR;
            this.e = e;
        }

        public String getE() {
            return e;
        }

        public void setE(String e) {
            this.e = e;
        }
    }

    public String getV() {
        return v;
    }

    public void setV(String v) {
        this.v = v;
    }

    public String getT() {
        return t;
    }

    public void setT(String t) {
        this.t = t;
    }

    public char getY() {
        return y;
    }

    public void setY(char y) {
        this.y = y;
    }

    /*
      ping Query = {"t":"aa", "y":"q", "q":"ping", "a":{"id":"abcdefghij0123456789"}}
      bencoded = d1:ad2:id20:abcdefghij0123456789e1:q4:ping1:t2:aa1:y1:qe

      Response = {"t":"aa", "y":"r", "r": {"id":"mnopqrstuvwxyz123456"}}
      bencoded = d1:rd2:id20:mnopqrstuvwxyz123456e1:t2:aa1:y1:re
     */
    /* @return bencoded string
     */
    public static ByteBuffer createPingRequest(String transactionId, String senderNodeId) throws IOException, UnsupportedEncodingException {
        Map<String, BEValue> pingQueryMap = new HashMap<String, BEValue>();
        pingQueryMap.put("t", new BEValue(transactionId));
        pingQueryMap.put("y", new BEValue("q"));
        pingQueryMap.put("q", new BEValue("ping"));
        Map<String, BEValue> senderNodeIdMap = new HashMap<String, BEValue>();
        senderNodeIdMap.put("id", new BEValue(senderNodeId));
        pingQueryMap.put("a", new BEValue(senderNodeIdMap));

        return BEncoder.bencode(pingQueryMap);
    }

    /*
      find_node Query = {"t":"aa", "y":"q", "q":"find_node", "a": {"id":"abcdefghij0123456789", "target":"mnopqrstuvwxyz123456"}}
      bencoded = d1:ad2:id20:abcdefghij01234567896:target20:mnopqrstuvwxyz123456e1:q9:find_node1:t2:aa1:y1:qe

      Response = {"t":"aa", "y":"r", "r": {"id":"0123456789abcdefghij", "nodes": "def456..."}}
      bencoded = d1:rd2:id20:0123456789abcdefghij5:nodes9:def456...e1:t2:aa1:y1:re

     */
    public static ByteBuffer createFindNodeRequest(String transactionId, String senderNodeId, String targetNodeId) throws IOException {
        Map<String, BEValue> findNodeQueryMap = new HashMap<String, BEValue>();
        findNodeQueryMap.put("t", new BEValue(transactionId));
        findNodeQueryMap.put("y", new BEValue("q"));
        findNodeQueryMap.put("q", new BEValue("findNode"));
        Map<String, BEValue> nodesMap = new HashMap<String, BEValue>();
        nodesMap.put("id", new BEValue(senderNodeId));
        nodesMap.put("target", new BEValue(targetNodeId));
        findNodeQueryMap.put("a", new BEValue(nodesMap));

        return BEncoder.bencode(findNodeQueryMap);
    }


    public static KMessage parseMessage(ByteBuffer data) {
        return null;
    }

    public static void main(String[] args) throws Exception {
        // expected: d1:ad2:id20:abcdefghij0123456789e1:q4:ping1:t2:aa1:y1:qe
        // generated: d1:ad2:id20:abcdefghij0123456789e1:q4:ping1:t2:aa1:y1:qe --> Bingo!
        System.out.println(createPingRequest("aa", "abcdefghij0123456789"));
    }
}
