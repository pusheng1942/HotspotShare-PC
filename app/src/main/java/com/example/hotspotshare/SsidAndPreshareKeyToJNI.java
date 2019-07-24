package com.example.hotspotshare;

public class SsidAndPreshareKeyToJNI {
    static {
        System.loadLibrary("native-lib");
    }

    public static native String transmitSsidAndPreshareKeyToJNI(String name);
}


