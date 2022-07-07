package dev.franckyi.kbuttplug.impl;

import com.sun.jna.IntegerType;

/**
 * Java wrapper for the Rust type {@code i32} (32-bit signed integer).
 */
public class i32 extends IntegerType {
    public i32() {
        this(0);
    }

    public i32(long value) {
        super(4, value, false);
    }
}
