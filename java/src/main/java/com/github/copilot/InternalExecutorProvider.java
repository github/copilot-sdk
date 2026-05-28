/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

final class InternalExecutorProvider {

    private final Executor executor;

    InternalExecutorProvider(Executor userProvided) {
        if (userProvided != null) {
            this.executor = userProvided;
        } else {
            this.executor = ForkJoinPool.commonPool();
        }
    }

    Executor get() {
        return executor;
    }

    boolean canBeShutdown() {
        // Since we are using ForkJoinPool.commonPool() or user provided only, 
        // we should not attempt to shut it down
        return false;
    }

}
