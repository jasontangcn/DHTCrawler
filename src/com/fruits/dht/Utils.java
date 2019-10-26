package com.fruits.dht;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
    static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static byte[] getSHA1(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(data);
        return md.digest();
    }

    public static String bytes2HexString(byte[] data) {
        if (data == null) return null;
        String hexChars = "0123456789abcdef";
        StringBuilder sb = new StringBuilder();
        for (byte v : data) {
            sb.append(hexChars.charAt((v >>> 4) & 0x0f));
            sb.append(hexChars.charAt(v & 0x0f));
        }
        return sb.toString();
    }

    public static byte[] hexStringToBytes(String hexString) {
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static final AtomicInteger transactionId = new AtomicInteger(1);

    public static String generateTransactionId() {
        int id = transactionId.getAndIncrement();
        byte[] bytes = new byte[2];
        bytes[1] = (byte) (id & 0xff);
        bytes[0] = (byte) (id >> 8 & 0xff);
        return new String(bytes, Charset.forName("ISO-8859-1"));
    }

    // random 20 bytes(160 bits)
    public static String generateNodeId() throws NoSuchAlgorithmException {
        Map<String, String> env = System.getenv();
        String userDomain = env.get("USERDOMAIN");
        String computerName = env.get("COMPUTERNAME");
        String userName = env.get("USERNAME");

        StringBuffer sb = new StringBuffer();

        sb.append("FRT-");

        if (userDomain != null && userDomain.length() > 0) {
            sb.append(userDomain).append("-");
        }

        if (computerName != null && computerName.length() > 0) {
            sb.append(computerName).append("-");
        }

        sb.append(UUID.randomUUID().toString());

        return bytes2HexString(getSHA1(sb.toString().getBytes()));
    }

    // TODO: log2?
    // nodeId : 160 bits

    // TODO: all of the bits of nodeId is 0, how to handle it?
    public static int getLog2(byte[] nodeId) {
        int length = nodeId.length * 8;
        Bitmap bitmap = new Bitmap(length);
        int i = 0;
        for(; i < length; i++) {
            if(bitmap.get(i))
              break;
        }

        return (length - 1 - i); // get the minIndex
    }

    public static byte[] getDistance(String nodeId1, String nodeId2) {
        // XOR
        byte[] n1Bytes = Utils.hexStringToBytes(nodeId1);
        byte[] n2Bytes = Utils.hexStringToBytes(nodeId2);

        byte[] distance = new byte[n1Bytes.length];

        for(int i = 0; i < n1Bytes.length; i++) {
            distance[i] = (byte)(n1Bytes[i] ^ n2Bytes[i]);
        }

        return distance;
    }

    /*
    Contact Encoding
    Contact information for peers is encoded as a 6-byte string.
    Alos known as "Compact IP-address/port info"
    the 4-byte IP address is in network byte order with the 2 byte port in network byte order concatenated onto the end.

    Contact information for nodes is encoded as a 26-byte string.
    Also known as "Compact node info" the 20-byte Node ID in network byte order has the compact IP-address/port info concatenated to the end.
     */
    public static List<Node> parseCompactNodes(String nodes) throws IOException {
        List<Node> nodesList = new ArrayList<Node>();

        if(nodes != null && nodes.length() > 0) {
            byte[] bytes = nodes.getBytes(); // TODO: bytes.length == nodes.length()?
            for(int i = 0; i < bytes.length/26; i++) {
                byte[] nodeIdBytes = Arrays.copyOfRange(bytes, i * 26, (i * 26 + 20));
                byte[] ipBytes = Arrays.copyOfRange(bytes, i * 26 + 20, (i * 26 + 24));
                byte[] portBytes = Arrays.copyOfRange(bytes, i * 26 + 24, (i + 1) * 26);

                InetAddress ip = InetAddress.getByAddress(ipBytes);
                ByteArrayInputStream bis = new ByteArrayInputStream(portBytes);
                DataInputStream dis = new DataInputStream(bis);
                int port = dis.readUnsignedShort();

                Node node = new Node(new String(nodeIdBytes), new InetSocketAddress(ip, port));
                nodesList.add(node);
            }
        }

        return nodesList;
    }


    // TODO: test this function and above parseCompactNodes!!!
    // every node should have any least two fields:
    // nodeId and address(hostname + port)
    public static String encodeCompactNodes(List<Node> nodes) {
        byte[] nodesBytes = new byte[nodes.size() * 26];

        for(int i = 0; i < nodes.size(); i++) {
            // TODO: need to verify it
            Node node = nodes.get(i);
            byte[] nodeId = node.getId().getBytes();
            byte[] hostname = node.getAddress().getAddress().getAddress();
            int port = node.getAddress().getPort();
            byte[] portBytes = new byte[2];
            portBytes[0] = (byte)((port & 0xFF00) >> 8);
            portBytes[1] = (byte)(port & 0xFF);

            System.arraycopy(nodeId, 0, nodesBytes, i * 26, nodeId.length);
            System.arraycopy(hostname, 0, nodesBytes, i * 26 + 20, hostname.length);
            System.arraycopy(portBytes, 0, nodesBytes, i * 26 + 24, portBytes.length);
        }

        return new String(nodesBytes, Charset.forName("ISO-8859-1"));
    }

    public static List<InetSocketAddress> parseCompactPeers(List<String> peers) throws IOException {
        List<InetSocketAddress> peersList = new ArrayList<InetSocketAddress>();

        for(String peer : peers) {
            byte[] bytes = peer.getBytes();
            if(bytes.length == 6) {
                byte[] ipBytes = Arrays.copyOfRange(bytes, 0, 4);
                byte[] portBytes = Arrays.copyOfRange(bytes, 4, 6);
                InetAddress ip = InetAddress.getByAddress(ipBytes);
                ByteArrayInputStream bis = new ByteArrayInputStream(portBytes);
                DataInputStream dis = new DataInputStream(bis);
                int port = dis.readUnsignedShort();
                peersList.add(new InetSocketAddress(ip, port));
            }
        }

        return peersList;
    }

    // create a peer id : length of 20 bytes(160 bits)
    public static byte[] createPeerId() {
        ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.put("-FRT-".getBytes());

        String ip = "";
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            logger.error("", e);
        }
        Map<String, String> env = System.getenv();
        String userDomain = env.get("USERDOMAIN");
        String computerName = env.get("COMPUTERNAME");
        String userName = env.get("USERNAME");

        StringBuffer sb = new StringBuffer();

        if (userDomain != null && userDomain.length() > 0) {
            sb.append(userDomain).append("-");
        }

        if (computerName != null && computerName.length() > 0) {
            sb.append(computerName).append("-");
        }

        if (userName != null && userName.length() > 0) {
            sb.append(userName);
        }

        byte[] bytes = sb.toString().getBytes();
        for (byte bt : bytes) {
            if (buffer.hasRemaining()) {
                buffer.put(bt);
            } else {
                break;
            }
        }

        while (buffer.hasRemaining()) {
            buffer.putInt((byte) '-');
        }
        return buffer.array();
    }

    // TODO: Rewrite, looks this code is too complex.
    public static ByteBuffer readFile(String filePath) throws IOException {
        FileInputStream fis = null;
        FileChannel channel = null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            File file = new File(filePath);
            if (!file.exists())
                return null;

            fis = new FileInputStream(file);
            channel = fis.getChannel();

            ByteBuffer buffer = ByteBuffer.allocate(1024);

            while (-1 != channel.read(buffer)) {
                buffer.flip();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                os.write(bytes);
                buffer.clear();
            }
        } finally {
            if (channel != null && channel.isOpen()) {
                try {
                    channel.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
            }

            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
            }

            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
        }

        return ByteBuffer.wrap(os.toByteArray());
    }

    public static CharBuffer readTextFile(String filePath, String charset) throws IOException, CharacterCodingException {
        ByteBuffer content = readFile(filePath);
        CharsetDecoder decoder = Charset.forName(charset).newDecoder();
        return decoder.decode(content);
    }

    public static void main(String[] args) throws Exception {
        // test generateTransactionId
        String txId1 = generateTransactionId();
        String txId2 = generateTransactionId();
        String txId3 = generateTransactionId();
        String txId4 = generateTransactionId();
        String txId5 = generateTransactionId();

        System.out.println(txId1 + "," + txId2 + "," + txId3 + "," + txId4 + "," + txId5);
        System.out.println(txId1.equals(txId1));
        System.out.println(txId1.equals(txId2));
        System.out.println(txId1.length());


        //
        for (int i = 0; i < 10; i++) {
            System.out.println(generateNodeId());
        }
    }
}
