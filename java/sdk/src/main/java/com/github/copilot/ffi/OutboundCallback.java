/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

/**
 * JNA callback interface for the runtime-to-Java outbound data path.
 *
 * <p>
 * The native runtime invokes this callback on a native thread when data is
 * ready to be delivered to the Java side. JNA automatically attaches the native
 * thread to the JVM before dispatching the callback.
 *
 * <p>
 * <strong>Buffer lifetime:</strong> The {@code data} pointer is only valid for
 * the duration of the callback invocation. Implementations must copy the bytes
 * out (e.g. {@code data.getByteArray(0, len)}) before returning.
 *
 * <p>
 * <strong>GC protection:</strong> Instances must be held as strong-reference
 * fields for as long as native code may invoke the callback. If the instance is
 * garbage-collected, the function pointer becomes dangling and the JVM will
 * crash.
 */
@FunctionalInterface
interface OutboundCallback extends Callback {

    /**
     * Invoked by the native runtime when outbound data is available.
     *
     * @param userData
     *            opaque cookie passed through unchanged from
     *            {@code copilot_runtime_connection_open}; always
     *            {@code Pointer.NULL} in this SDK
     * @param data
     *            pointer to the outbound byte buffer; valid only for the duration
     *            of this invocation
     * @param len
     *            byte length of the buffer pointed to by {@code data}
     */
    void invoke(Pointer userData, Pointer data, int len);
}
