package com.fruits.dht.test;

public class TypeCastTest {
    public static void main(String[] args) {
        byte a = -10;
        // 32 bits
        System.out.println(0xFFFFFFFE);
        System.out.println(0xF8);

        int b = 300;
        byte c = (byte)b;

        System.out.println(c);
    }
}
