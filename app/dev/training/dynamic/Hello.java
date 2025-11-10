package dev.training.dynamic;

import android.content.Context;

public class Hello {
    // Option A: common pattern the app may reflect on
    public static String greet() {
        return "Hello from dynamic DEX";
    }

    // Option B: sometimes apps expect a Context param
    public static String run(Context ctx) {
        return "Dynamic run() loaded OK";
    }
}
