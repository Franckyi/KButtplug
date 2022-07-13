package dev.franckyi.kbuttplug.impl;

import com.sun.jna.IntegerType;

/**
 * Java wrapper for the Rust type {@code u32} (32-bit unsigned integer).
 * This class is public only because JNA needs to access it though reflection.
 */
public class u32 extends IntegerType {
    /**
     * Public default constructor required by JNA to instantiate the class.
     */
    public u32() {
        this(0);
    }

    u32(long value) {
        super(4, value, true);
    }
}
