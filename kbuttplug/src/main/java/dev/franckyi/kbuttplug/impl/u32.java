package dev.franckyi.kbuttplug.impl;

import com.sun.jna.IntegerType;

/**
 * Java wrapper for the Rust type {@code u32} (32-bit unsigned integer).
 */
public class u32 extends IntegerType {
    public u32() {
        this(0);
    }

    public u32(long value) {
        super(4, value, true);
    }
}
