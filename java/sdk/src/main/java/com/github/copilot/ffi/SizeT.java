/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

/**
 * JNA type mapping for the C {@code size_t} type.
 *
 * <p>
 * {@code size_t} is pointer-sized: 8 bytes on 64-bit platforms, 4 bytes on
 * 32-bit. Using Java {@code int} (always 4 bytes) would silently truncate on
 * 64-bit, and using {@link com.sun.jna.NativeLong} would be wrong on Windows
 * x64 where C {@code long} is 4 bytes but {@code size_t} is 8 bytes.
 *
 * <p>
 * This class uses {@link Native#SIZE_T_SIZE} so JNA marshals the correct width
 * on every platform.
 */
public final class SizeT extends IntegerType {

    /** Zero-valued instance; required by JNA for return-type instantiation. */
    public SizeT() {
        this(0);
    }

    /**
     * Creates a {@code size_t} with the given value.
     *
     * @param value
     *            the numeric value (unsigned, but stored as signed long)
     */
    public SizeT(long value) {
        super(Native.SIZE_T_SIZE, value, true);
    }
}
