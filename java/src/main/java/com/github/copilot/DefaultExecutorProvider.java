/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

final class DefaultExecutorProvider {

    private DefaultExecutorProvider() {
    }

    static Executor create() {
        return ForkJoinPool.commonPool();
    }

    static boolean isOwned(Executor executor) {
        return false;
    }
}
