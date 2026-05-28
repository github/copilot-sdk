/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

final class InternalExecutorProvider {

    private final Executor executor;
    private final boolean owned;

    InternalExecutorProvider(Executor userProvided) {
        if (userProvided != null) {
            this.executor = userProvided;
            this.owned = false;
        } else {
            this.executor = Executors.newVirtualThreadPerTaskExecutor();
            this.owned = true;
        }
    }

    Executor get() {
        return executor;
    }

    boolean canBeShutdown() {
        // We can only shut down the executor if we created it (i.e., if it's owned) 
        // such as when using Executors.newVirtualThreadPerTaskExecutor(), 
        // which creates an executor that we are responsible for shutting down.
        return owned;
    }
}
