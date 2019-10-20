package com.fruits.dht.krpc;

import com.turn.torrent.bcodec.BEValue;
import com.turn.torrent.bcodec.BEncoder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class KMessage {
    // TODO: tx id contains contrl characters.
    public String t; // 2 characters(2 bytes) transaction id
    public char y; // y => q or r or e
    public String q;
    public String e;
    public Map a;
    public Map r;

    public char v;

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
    public static ByteBuffer createFindNodeRequest(String transactionId, String senderNodeId, String targetNodeId) throws IOException, UnsupportedEncodingException {
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

    }

    public static void main(String[] args) throws Exception {
        //  expected: d1:ad2:id20:abcdefghij0123456789e1:q4:ping1:t2:aa1:y1:qe
        // generated: d1:ad2:id20:abcdefghij0123456789e1:q4:ping1:t2:aa1:y1:qe --> Bingo!
        System.out.println(createPingRequest("aa", "abcdefghij0123456789"));
    }
}
