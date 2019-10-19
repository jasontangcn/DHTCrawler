package com.fruits.dht.krpc;

import java.util.Map;

public class KMessage {
    public char t; // transaction id
    public char y; // y => q or r or e
    public String q;

    public String e;
    public Map a;
    public Map r;

    public char v;
}
