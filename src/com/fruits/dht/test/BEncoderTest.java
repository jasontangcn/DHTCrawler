package com.fruits.dht.test;

import com.turn.torrent.bcodec.BDecoder;
import com.turn.torrent.bcodec.BEValue;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Map;

public class BEncoderTest {
    public static void main(String[] args) throws Exception {
        /*
        find_node Query = {"t":"aa", "y":"q", "q":"find_node", "a": {"id":"abcdefghij0123456789", "target":"mnopqrstuvwxyz123456"}}
        bencoded = d1:ad2:id20:abcdefghij01234567896:target20:mnopqrstuvwxyz123456e1:q9:find_node1:t2:aa1:y1:qe

        Response = {"t":"aa", "y":"r", "r": {"id":"0123456789abcdefghij", "nodes": "def456..."}}
        bencoded = d1:rd2:id20:0123456789abcdefghij5:nodes9:def456...e1:t2:aa1:y1:re
         */

        // how to decode?
        byte[] findNodeQueryBytes = "d1:ad2:id20:abcdefghij01234567896:target20:mnopqrstuvwxyz123456e1:q9:find_node1:t2:aa1:y1:qe".getBytes();
        Map findNodeQueryMap = BDecoder.bdecode(ByteBuffer.wrap(findNodeQueryBytes)).getMap();
        String t = ((BEValue) findNodeQueryMap.get("t")).getString();
        String y = ((BEValue) findNodeQueryMap.get("y")).getString();
        String q = ((BEValue) findNodeQueryMap.get("q")).getString();
        Map findNodeQueryAMap = ((BEValue) findNodeQueryMap.get("a")).getMap();
        String id = ((BEValue) findNodeQueryAMap.get("id")).getString();
        String target = ((BEValue) findNodeQueryAMap.get("target")).getString();
        System.out.print("t = " + t + ", y = " + y + ", q = " + q + ", id = " + id + ", target = " + target + ".");

        // how to encode?
        // String
        // Number
        // List-> List<BEValue>
        // Map-> Map<String, BEValue>
        // byte[]

    }
}
