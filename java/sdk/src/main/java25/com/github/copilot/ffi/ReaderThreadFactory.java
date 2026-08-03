/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

/**
 * JDK 25 multi-release variant of {@link ReaderThreadFactory}.
 */
final class ReaderThreadFactory {

    Thread create(Runnable task, String name) {
        return Thread.ofVirtual().name(name).unstarted(task);
    }
}
