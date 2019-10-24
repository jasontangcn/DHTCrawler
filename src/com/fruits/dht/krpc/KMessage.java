package com.fruits.dht.krpc;

import com.fruits.dht.DHTManager;
import com.turn.torrent.bcodec.BDecoder;
import com.turn.torrent.bcodec.BEValue;
import com.turn.torrent.bcodec.BEncoder;

import javax.management.Query;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.*;

public abstract class KMessage {
    public static final String KMESSAGE_T = "t";
    public static final String KMESSAGE_Y = "y";
    public static final String KMESSAGE_QUERY_A = "a";
    public static final String KMESSAGE_RESPONSE_R = "r";

    public static final String KMESSAGE_Y_QUERY = "q";
    public static final String KMESSAGE_Y_RESPONSE = "r";
    public static final String KMESSAGE_Y_ERROR = "e";

    // for y = 'q'
    public static final String KMESSAGE_QUERY_PING = "ping";
    public static final String KMESSAGE_QUERY_FIND_NODE = "find_node";
    public static final String KMESSAGE_QUERY_GET_PEERS = "get_peers";
    public static final String KMESSAGE_QUERY_ANNOUNCE_PEER = "announce_peer";

    public static final String KMESSAGE_KEY_ID = "id";
    public static final String KMESSAGE_QUERY_KEY_TARGET = "target";
    public static final String KMESSAGE_QUERY_KEY_INFO_HASH = "info_hash";
    public static final String KMESSAGE_QUERY_KEY_IMPLIED_PORT = "implied_port";
    public static final String KMESSAGE_QUERY_KEY_PORT = "port";
    public static final String KMESSAGE_QUERY_KEY_TOKEN = "token";

    public static final String KMESSAGE_RESPONSE_KEY_NODES = "nodes";
    public static final String KMESSAGE_RESPONSE_KEY_VALUES = "values";

    public static final String KMESSAGE_ERROR_KEY_E = "e";

    protected String v; // optional
    // TODO: tx id contains contrl characters.
    protected String t; // 2 characters(2 bytes) transaction id
    protected String y; // y => q or r or e
    //public String q; -> query
    //public Map a; -> query

    //public String e; -> error

    //public Map r; -> response

    // transaction id - every message has it.
    public KMessage(String t) {
        this.t = t;
    }

    public static abstract class Query extends KMessage {
        protected String q;
        protected Map<String, String> a;

        public Query(String t) {
            super(t);
            this.y = KMESSAGE_Y_QUERY; // y ->
            this.a = new HashMap<String, String>();
        }

        public Query(String t, Map<String, String> a) {
            super(t);
            this.y = KMESSAGE_Y_QUERY; // y ->
            this.a = a;
        }

        public String getQ() {
            return q;
        }

        public void setQ(String q) {
            this.q = q;
        }

        public Map<String, String> getA() {
            return a;
        }

        public String putA(String key, String value) {
            return a.put(key, value);
        }

        public ByteBuffer bencode() throws IOException {
            Map<String, BEValue> queryMap = new HashMap<String, BEValue>();
            queryMap.put(KMESSAGE_T, new BEValue(getT()));
            queryMap.put(KMESSAGE_Y, new BEValue(getY()));
            queryMap.put(KMESSAGE_Y_QUERY, new BEValue(getQ()));
            Map<String, BEValue> queryAMap = new HashMap<String, BEValue>();
            for(Map.Entry<String, String> entry : getA().entrySet()) {
                String key = entry.getKey();
                // special handling for int values.
                if(key.equals(KMESSAGE_QUERY_KEY_IMPLIED_PORT) || key.equals(KMESSAGE_QUERY_KEY_PORT)) {
                    int port = Integer.parseInt(entry.getValue());
                    queryAMap.put(entry.getKey(), new BEValue(port));
                }else {
                    queryAMap.put(entry.getKey(), new BEValue(entry.getValue()));
                }
            }
            queryMap.put(KMESSAGE_QUERY_A, new BEValue(queryAMap));

            return BEncoder.bencode(queryMap);
        }
    }

    /*
    ping Query = {"t":"aa", "y":"q", "q":"ping", "a":{"id":"abcdefghij0123456789"}}
    bencoded = d1:ad2:id20:abcdefghij0123456789e1:q4:ping1:t2:aa1:y1:qe

    Response = {"t":"aa", "y":"r", "r": {"id":"mnopqrstuvwxyz123456"}}
    bencoded = d1:rd2:id20:mnopqrstuvwxyz123456e1:t2:aa1:y1:re
    */
    public static class PingQuery extends Query {
        public PingQuery(String t, Map<String, String> a) {
            super(t, a);
            this.q = KMESSAGE_QUERY_PING;
        }

        public PingQuery(String t, String id) {
            super(t);
            this.q = KMESSAGE_QUERY_PING;
            putA(KMESSAGE_KEY_ID, id);
        }
    }

    /*
    find_node Query = {"t":"aa", "y":"q", "q":"find_node", "a": {"id":"abcdefghij0123456789", "target":"mnopqrstuvwxyz123456"}}
    bencoded = d1:ad2:id20:abcdefghij01234567896:target20:mnopqrstuvwxyz123456e1:q9:find_node1:t2:aa1:y1:qe

    Response = {"t":"aa", "y":"r", "r": {"id":"0123456789abcdefghij", "nodes": "def456..."}}
    bencoded = d1:rd2:id20:0123456789abcdefghij5:nodes9:def456...e1:t2:aa1:y1:re
     */
    public static class FindNodeQuery extends Query {
        public FindNodeQuery(String t, Map<String, String> a) {
            super(t, a);
            this.q = KMESSAGE_QUERY_FIND_NODE;
        }

        public FindNodeQuery(String t, String id, String target) {
            super(t);
            this.q = KMESSAGE_QUERY_FIND_NODE;
            putA(KMESSAGE_KEY_ID, id);
            putA(KMESSAGE_QUERY_KEY_TARGET, target);
        }
    }

    /*
    get_peers Query = {"t":"aa", "y":"q", "q":"get_peers", "a": {"id":"abcdefghij0123456789", "info_hash":"mnopqrstuvwxyz123456"}}
    bencoded = d1:ad2:id20:abcdefghij01234567899:info_hash20:mnopqrstuvwxyz123456e1:q9:get_peers1:t2:aa1:y1:qe

    Response with peers = {"t":"aa", "y":"r", "r": {"id":"abcdefghij0123456789", "token":"aoeusnth", "values": ["axje.u", "idhtnm"]}}
    bencoded = d1:rd2:id20:abcdefghij01234567895:token8:aoeusnth6:valuesl6:axje.u6:idhtnmee1:t2:aa1:y1:re

    Response with closest nodes = {"t":"aa", "y":"r", "r": {"id":"abcdefghij0123456789", "token":"aoeusnth", "nodes": "def456..."}}
    bencoded = d1:rd2:id20:abcdefghij01234567895:nodes9:def456...5:token8:aoeusnthe1:t2:aa1:y1:re
    */
    public static class GetPeersQuery extends Query {
        public GetPeersQuery(String t, Map<String, String> a) {
            super(t, a);
            this.q = KMESSAGE_QUERY_GET_PEERS;
        }

        public GetPeersQuery(String t, String id, String infoHash) {
            super(t);
            this.q = KMESSAGE_QUERY_GET_PEERS;
            putA(KMESSAGE_KEY_ID, id);
            putA(KMESSAGE_QUERY_KEY_INFO_HASH, infoHash);
        }
    }

    /*
    announce_peers Query = {"t":"aa", "y":"q", "q":"announce_peer", "a": {"id":"abcdefghij0123456789", "implied_port": 1, "info_hash":"mnopqrstuvwxyz123456", "port": 6881, "token": "aoeusnth"}}
    bencoded = d1:ad2:id20:abcdefghij012345678912:implied_porti1e9:info_hash20:mnopqrstuvwxyz1234564:porti6881e5:token8:aoeusnthe1:q13:announce_peer1:t2:aa1:y1:qe

    Response = {"t":"aa", "y":"r", "r": {"id":"mnopqrstuvwxyz123456"}}
    bencoded = d1:rd2:id20:mnopqrstuvwxyz123456e1:t2:aa1:y1:re
     */

    public static class AnnouncePeerQuery extends Query {
        public AnnouncePeerQuery(String t, Map<String, String> a) {
            super(t, a);
            this.q = KMESSAGE_QUERY_ANNOUNCE_PEER;
        }

        public AnnouncePeerQuery(String t, String id, int impliedPort, String infoHash, int port, String token) {
            super(t);
            this.q = KMESSAGE_QUERY_ANNOUNCE_PEER;
            putA(KMESSAGE_KEY_ID, id);
            putA(KMESSAGE_QUERY_KEY_IMPLIED_PORT, String.valueOf(impliedPort));
            putA(KMESSAGE_QUERY_KEY_INFO_HASH, infoHash);
            putA(KMESSAGE_QUERY_KEY_PORT, String.valueOf(port));
            putA(KMESSAGE_QUERY_KEY_TOKEN, token);
        }
    }

    // although different response has different 'a' but there is
    // no special flag to indicate the response type(a response for ping or a response for get_peers)
    // need to find the corresponding request by transaction id.
    public static abstract class Response extends KMessage {
        protected Map<String, Object> r;

        public Response(String t) {
            super(t);
            this.y = KMESSAGE_Y_RESPONSE;
            this.r = new HashMap<String, Object>();
        }

        public Response(String t, Map<String, Object> r) {
            super(t);
            this.y = KMESSAGE_Y_RESPONSE;
            this.r = r;
        }

        public Map<String, Object> getR() {
            return r;
        }

        public Object getR(String key) {
            return r.get(key);
        }

        public Object putR(String key, Object value) {
            return this.r.put(key, value);
        }

        public ByteBuffer bencode() throws IOException {
            Map<String, BEValue> responseMap = new HashMap<String, BEValue>();
            responseMap.put(KMESSAGE_T, new BEValue(getT()));
            responseMap.put(KMESSAGE_Y, new BEValue(getY()));
            Map<String, BEValue> responseRMap = new HashMap<String, BEValue>();
            for(Map.Entry<String, Object> entry : getR().entrySet()) {
                // expect response of get_peers, the R map are <String, String>
                responseRMap.put(entry.getKey(), new BEValue(entry.getValue().toString()));
            }
            responseMap.put(KMESSAGE_RESPONSE_R, new BEValue(responseRMap));

            return BEncoder.bencode(responseMap);
        }
    }

    public static class PingResponse extends Response {
        public PingResponse(String t, String id) {
            super(t);
            putR(KMESSAGE_KEY_ID, id);
        }

        public PingResponse(String t, Map<String, Object> r) {
            super(t, r);
        }
    }

    public static class FindNodeResponse extends Response {
        public FindNodeResponse(String t, String id, String nodes) {
            super(t);
            putR(KMESSAGE_KEY_ID, id);
            putR(KMESSAGE_RESPONSE_KEY_NODES, nodes);
        }

        public FindNodeResponse(String t, Map<String, Object> r) {
            super(t, r);
        }
    }

    /*
    Response with peers = {"t":"aa", "y":"r", "r": {"id":"abcdefghij0123456789", "token":"aoeusnth", "values": ["axje.u", "idhtnm"]}}
    bencoded = d1:rd2:id20:abcdefghij01234567895:token8:aoeusnth6:valuesl6:axje.u6:idhtnmee1:t2:aa1:y1:re

    Response with closest nodes = {"t":"aa", "y":"r", "r": {"id":"abcdefghij0123456789", "token":"aoeusnth", "nodes": "def456..."}}
    bencoded = d1:rd2:id20:abcdefghij01234567895:nodes9:def456...5:token8:aoeusnthe1:t2:aa1:y1:re
    */
    public static class GetPeersResponse extends Response {
        public GetPeersResponse(String t, String id, String token, List<String> values) {
            super(t);
            putR(KMESSAGE_KEY_ID, id);
            putR(KMESSAGE_QUERY_KEY_TOKEN, token);
            putR(KMESSAGE_RESPONSE_KEY_VALUES, values);
        }

        public GetPeersResponse(String t, String id, String token, String nodes) {
            super(t);
            putR(KMESSAGE_KEY_ID, id);
            putR(KMESSAGE_QUERY_KEY_TOKEN, token);
            putR(KMESSAGE_RESPONSE_KEY_NODES, nodes);
        }

        public GetPeersResponse(String t, Map<String, Object> r) {
            super(t, r);
        }

        public ByteBuffer bencode() throws IOException {
            Map<String, BEValue> responseMap = new HashMap<String, BEValue>();
            responseMap.put(KMESSAGE_T, new BEValue(getT()));
            responseMap.put(KMESSAGE_Y, new BEValue(getY()));
            Map<String, BEValue> responseRMap = new HashMap<String, BEValue>();
            for(Map.Entry<String, Object> entry : getR().entrySet()) {
                // expect response of get_peers, the R map are <String, String>
                String key = entry.getKey();
                if(KMESSAGE_RESPONSE_KEY_VALUES.equals(key)) {
                    List<BEValue> bevalues = new ArrayList<BEValue>();
                    List<String> strings = (List<String>)entry.getValue();
                    for(String string : strings) {
                        bevalues.add(new BEValue(string));
                    }
                    responseRMap.put(entry.getKey(), new BEValue(bevalues));
                }else {
                    responseRMap.put(entry.getKey(), new BEValue(entry.getValue().toString()));
                }
            }
            responseMap.put(KMESSAGE_RESPONSE_R, new BEValue(responseRMap));

            return BEncoder.bencode(responseMap);
        }
    }


    public static class AnnouncePeerResponse extends Response {
        public AnnouncePeerResponse(String t, String id) {
            super(t);
            putR(KMESSAGE_KEY_ID, id);
        }

        public AnnouncePeerResponse(String t, Map<String, Object> r) {
            super(t, r);
        }
    }

    // t
    // y -> q, r, e
    // q -> ping, find_node, get_peers, announce_peer
    // a for q
    // r for r
    // e ror e

    // queries -> transaction id -> Query
    public static KMessage parseKMessage(ByteBuffer data, Map<String, Query> queries) throws IOException {
        Map<String, BEValue> map = BDecoder.bdecode(data).getMap();
        String t = map.get(KMESSAGE_T).getString();
        String y = map.get(KMESSAGE_Y).getString();

        if(y.equals(KMESSAGE_Y_QUERY)) {
            String q = map.get(KMESSAGE_Y_QUERY).toString();
            Map<String, BEValue> aMap = (Map<String, BEValue>)map.get(KMESSAGE_QUERY_A);
            String id = aMap.get(KMESSAGE_KEY_ID).getString();

            if(q.equals(KMESSAGE_QUERY_PING)) {
                return new PingQuery(t, id);
            }else if(q.equals(KMESSAGE_QUERY_FIND_NODE)) {
                String target = aMap.get(KMESSAGE_QUERY_KEY_TARGET).getString();
                return new FindNodeQuery(t, id, target);
            }else if(q.equals(KMESSAGE_QUERY_GET_PEERS)) {
                String infoHash = aMap.get(KMESSAGE_QUERY_KEY_INFO_HASH).getString();
                return new GetPeersQuery(t, id, infoHash);
            }else if(q.equals(KMESSAGE_QUERY_ANNOUNCE_PEER)) {
                int impliedPort = aMap.get(KMESSAGE_QUERY_KEY_IMPLIED_PORT).getInt();
                String infoHash = aMap.get(KMESSAGE_QUERY_KEY_INFO_HASH).getString();
                int port = aMap.get(KMESSAGE_QUERY_KEY_PORT).getInt();
                String token = aMap.get(KMESSAGE_QUERY_KEY_TOKEN).getString();
                // Query->a <String, String>
                // Encode query -> special handling -> String-> int
                return new AnnouncePeerQuery(t, id, impliedPort, infoHash, port, token);
            }
        }else if(y.equals(KMESSAGE_Y_RESPONSE)) {
            Map<String, BEValue> rMap = (Map<String, BEValue>)map.get(KMESSAGE_RESPONSE_R);
            String id = rMap.get(KMESSAGE_KEY_ID).getString();

            Query query = queries.get(t);
            if(query instanceof PingQuery) {
                return new PingResponse(t, id);
            }else if(query instanceof FindNodeQuery) {
                String nodes = rMap.get(KMESSAGE_RESPONSE_KEY_NODES).getString();
                return new FindNodeResponse(t, id, nodes);
            }else if(query instanceof GetPeersQuery) {
                String token = rMap.get(KMESSAGE_QUERY_KEY_TOKEN).toString();
                BEValue nodes = rMap.get(KMESSAGE_RESPONSE_KEY_NODES);
                if(nodes != null) {
                    return new GetPeersResponse(t, id, token, nodes.getString());
                }else{
                    List<BEValue> vs = rMap.get(KMESSAGE_RESPONSE_KEY_VALUES).getList();
                    List<String> values = new ArrayList<String>();
                    for(BEValue v : vs) {
                        values.add(v.getString());
                    }
                    return new GetPeersResponse(t, id, token, values);
                }
            }else if(query instanceof AnnouncePeerQuery) {
                return new AnnouncePeerResponse(t, id);
            }
        }else if(y.equals(KMESSAGE_Y_ERROR)) {
            String e = map.get(KMESSAGE_ERROR_KEY_E).toString();
            return new ErrorMessage(t, e);
        }

        return null;
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

    public String getY() {
        return y;
    }

    public void setY(String y) {
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
    /*
    public static ByteBuffer createPingRequest(String transactionId, String senderNodeId) throws IOException {
        Map<String, BEValue> pingQueryMap = new HashMap<String, BEValue>();
        pingQueryMap.put("t", new BEValue(transactionId));
        pingQueryMap.put("y", new BEValue("q"));
        pingQueryMap.put("q", new BEValue("ping"));
        Map<String, BEValue> senderNodeIdMap = new HashMap<String, BEValue>();
        senderNodeIdMap.put("id", new BEValue(senderNodeId));
        pingQueryMap.put("a", new BEValue(senderNodeIdMap));

        return BEncoder.bencode(pingQueryMap);
    }
    */

    /*
      find_node Query = {"t":"aa", "y":"q", "q":"find_node", "a": {"id":"abcdefghij0123456789", "target":"mnopqrstuvwxyz123456"}}
      bencoded = d1:ad2:id20:abcdefghij01234567896:target20:mnopqrstuvwxyz123456e1:q9:find_node1:t2:aa1:y1:qe

      Response = {"t":"aa", "y":"r", "r": {"id":"0123456789abcdefghij", "nodes": "def456..."}}
      bencoded = d1:rd2:id20:0123456789abcdefghij5:nodes9:def456...e1:t2:aa1:y1:re

     */
    /*
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
    */

    public static void main(String[] args) throws Exception {
        // expected: d1:ad2:id20:abcdefghij0123456789e1:q4:ping1:t2:aa1:y1:qe
        // generated: d1:ad2:id20:abcdefghij0123456789e1:q4:ping1:t2:aa1:y1:qe --> Bingo!
        // System.out.println(new String(createPingRequest("aa", "abcdefghij0123456789").array()));

        PingQuery pingQuery = new PingQuery("aa", "abcdefghij0123456789");
        System.out.println(new String(pingQuery.bencode().array()));
    }
}
