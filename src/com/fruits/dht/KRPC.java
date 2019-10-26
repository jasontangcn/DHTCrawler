package com.fruits.dht;

/*
  Each node has a globally unique identifier known as the "node ID."
  Node IDs are chosen at random from the same 160-bit space as BitTorrent infohashes.

  A "peer" is a client/server listening on a TCP port that implements the BitTorrent protocol.
  A "node" is a client/server listening on a UDP port implementing the distributed hash table protocol.
  The DHT is composed of nodes and stores the location of peers.
  BitTorrent clients include a DHT node, which is used to contact other nodes in the DHT
  to get the location of peers to download from using the BitTorrent protocol.


  KRPC Protocal
    Three message types: query, response, error

    Every message has a key "t" with a string value representing a transaction ID.
    The transaction ID should be encoded as a short string of binary numbers, typically 2 characters
    are enough as they cover 2^16 outstanding queries.

    Every message also has a key "y" with a single character value describing the type of message.
    The value of the "y" key is one of "q", "r", "e".

    A key "v" should be included in every message with a client version string. The string should be
    a two character client identifier registered in BEP 20 followed by a two character version identifier.
    Not all implementations include a "v" key so clients should not assume its presence.

    Contact Encoding
    Contact information for peers is encoded as a 6-byte string.
    Alos known as "Compact IP-address/port info"
    the 4-byte IP address is in network byte order with the 2 byte port in network byte order concatenated onto the end.

    Contact information for nodes is encoded as a 26-byte string.
    Also known as "Compact node info" the 20-byte Node ID in network byte order has the compact IP-address/port info concatenated to the end.


    generic error = {"t":"aa", "y":"e", "e":[201, "A Generic Error Ocurred"]}
    bencoded = d1:eli201e23:A Generic Error Ocurrede1:t2:aa1:y1:ee

    DHT Queries

    ping
    The most basic query is ping. "q" == "ping"
    A ping query has a single argument, "id" the value is a 20-byte string containing the senders
    node ID in network byte order. The appropriate response to a ping has a single key "id" containing
    the node ID of the responding node.

    ping Query = {"t":"aa", "y":"q", "q":"ping", "a":{"id":"abcdefghij0123456789"}}
    bencoded = d1:ad2:id20:abcdefghij0123456789e1:q4:ping1:t2:aa1:y1:qe

    Response = {"t":"aa", "y":"r", "r": {"id":"mnopqrstuvwxyz123456"}}
    bencoded = d1:rd2:id20:mnopqrstuvwxyz123456e1:t2:aa1:y1:re

    find_node
    Find node is used to find the contact information for a node given its ID. "q" == "find_node"
    A find_node query has two arguments, "id" containing the node ID of the querying node,
    and the "target" containing the ID of the node sought by the queryer.
    When a node receives a find_node query, it should respond with a key "nodes" and
    value of a string containing the compact node info for the target node or the K(8) closest good
    nodes in its own routing table.

    find_node Query = {"t":"aa", "y":"q", "q":"find_node", "a": {"id":"abcdefghij0123456789", "target":"mnopqrstuvwxyz123456"}}
    bencoded = d1:ad2:id20:abcdefghij01234567896:target20:mnopqrstuvwxyz123456e1:q9:find_node1:t2:aa1:y1:qe

    Response = {"t":"aa", "y":"r", "r": {"id":"0123456789abcdefghij", "nodes": "def456..."}}
    bencoded = d1:rd2:id20:0123456789abcdefghij5:nodes9:def456...e1:t2:aa1:y1:re

    get_peers
    Get peers associated with a torrent infohash. "q" == "get_peers"
    A get_peers query has two arguments, "id" containing the node ID of the querying node,
    and "info_hash" containing the infohash of the torrent.
    If the queried node has peers for the infohash, they are returned in a key "values" as a list
    of strings. Each string containing "compact" format peer information for a single peer.
    If the queried node has no peers for the infohash, a key "nodes" is returned containing the K nodes
    in the queried nodes routing table closest to the infohash supplied in the query.
    in either case a "token" key is also included in the retur value. The token value is required
    argument for a future announce_peer query. The token value should be a short binary string.

    get_peers Query = {"t":"aa", "y":"q", "q":"get_peers", "a": {"id":"abcdefghij0123456789", "info_hash":"mnopqrstuvwxyz123456"}}
    bencoded = d1:ad2:id20:abcdefghij01234567899:info_hash20:mnopqrstuvwxyz123456e1:q9:get_peers1:t2:aa1:y1:qe

    Response with peers = {"t":"aa", "y":"r", "r": {"id":"abcdefghij0123456789", "token":"aoeusnth", "values": ["axje.u", "idhtnm"]}}
    bencoded = d1:rd2:id20:abcdefghij01234567895:token8:aoeusnth6:valuesl6:axje.u6:idhtnmee1:t2:aa1:y1:re

    Response with closest nodes = {"t":"aa", "y":"r", "r": {"id":"abcdefghij0123456789", "token":"aoeusnth", "nodes": "def456..."}}
    bencoded = d1:rd2:id20:abcdefghij01234567895:nodes9:def456...5:token8:aoeusnthe1:t2:aa1:y1:re

    announce_peer
    Announce that the peer, controlling the querying node, is downloading a torrent on a port.
    announce_peer has four arguments: "id" containing the node ID of the querying node, "info_hash"
    containing the infohash of the torrent, "port" containing the port as an integer, and the "token"
    received in response to previous get_peers query. The queried node must verify that the token
    was previously sent to the same IP address as the querying node.
    Then the queried node should store the IP address of the querying node and the supplied port number
    under the infohash in its store of peer contact information.

    announce_peers Query = {"t":"aa", "y":"q", "q":"announce_peer", "a": {"id":"abcdefghij0123456789", "implied_port": 1, "info_hash":"mnopqrstuvwxyz123456", "port": 6881, "token": "aoeusnth"}}
    bencoded = d1:ad2:id20:abcdefghij012345678912:implied_porti1e9:info_hash20:mnopqrstuvwxyz1234564:porti6881e5:token8:aoeusnthe1:q13:announce_peer1:t2:aa1:y1:qe

    Response = {"t":"aa", "y":"r", "r": {"id":"mnopqrstuvwxyz123456"}}
    bencoded = d1:rd2:id20:mnopqrstuvwxyz123456e1:t2:aa1:y1:re


    Network order(big endian)


 */
public class KRPC {
    public static void ping() {

    }

    public static void findNode() {

    }

    public static void getPeers() {

    }

    public static void announcePeers() {

    }

}
