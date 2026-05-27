/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class DefaultExecutorProvider {

    private DefaultExecutorProvider() {
    }

    static Executor create() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    static boolean isOwned(Executor executor) {
        return executor instanceof ExecutorService;
    }
}
