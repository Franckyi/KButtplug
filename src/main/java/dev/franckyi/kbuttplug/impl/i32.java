package dev.franckyi.kbuttplug.impl;

import com.sun.jna.IntegerType;

/**
 * Java wrapper for the Rust type {@code i32} (32-bit signed integer).
 * This class is public only because JNA needs to access it though reflection.
 */
public class i32 extends IntegerType {
    /**
     * Public default constructor required by JNA to instantiate the class.
     */
    public i32() {
        this(0);
    }

    i32(long value) {
        super(4, value, false);
    }
}
