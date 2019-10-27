package com.fruits.dht.test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.InetAddress;

public class TypeCastTest {
    public static void main(String[] args) throws Exception {
        /*
        byte a = -10;
        // 32 bits
        System.out.println(0xFFFFFFFE);
        System.out.println(0xF8);

        int b = 300;
        byte c = (byte) b;

        System.out.println(c);
        */
        /*
        byte[] bytes = new byte[2];
        bytes[0] = (byte)27;
        bytes[1] = (byte)57;

        for(byte b : bytes) {
            System.out.print(byte2bits(b));
        }


        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bis);
        int port = dis.readUnsignedShort();
        System.out.println(port);
        System.out.println(bytes2int(bytes));
        */
        byte b = -2;

        System.out.println((byte)(b & 0xff));
    }


    public static String byte2bits(byte b) {
        int z = b; z |= 256;
        String str = Integer.toBinaryString(z);
        int len = str.length();
        return str.substring(len-8, len);
    }

    public static int bytes2int(byte[]  bytes) {
        int i;
        int a = bytes[1];
        int b = (bytes[0] & 0xFF) << 8;
        return a | b;

    }

    public static String byteToBinaryString(byte b) {

        return Integer.toBinaryString((b & 0xFF) + 0x100).substring(1);
    }
}
