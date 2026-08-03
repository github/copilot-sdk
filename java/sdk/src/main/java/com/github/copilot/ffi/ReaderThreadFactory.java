/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

/**
 * Creates reader threads for FFI queue consumption.
 *
 * <p>
 * Baseline (JDK 17) implementation creates a daemon platform thread. The JDK 25
 * multi-release overlay switches this to a virtual thread with the same
 * package-private API.
 */
final class ReaderThreadFactory {

    Thread create(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }
}
