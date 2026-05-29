/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Modifier;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.CopilotClientOptions;

class InternalExecutorProviderTest {

    @Test
    void baseProviderReturnsCommonPool() {
        Executor executor = new InternalExecutorProvider(null).get();

        assertSame(ForkJoinPool.commonPool(), executor);
    }

    @Test
    void userProvidedExecutorIsNotOwned() {
        Executor executor = ForkJoinPool.commonPool();

        assertFalse(new InternalExecutorProvider(executor).canBeShutdown());
    }

    @Test
    void providerIsPackagePrivate() {
        assertFalse(Modifier.isPublic(InternalExecutorProvider.class.getModifiers()));
    }

    @Test
    void clientDoesNotShutDownUserProvidedExecutor() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            try (var client = new CopilotClient(new CopilotClientOptions().setAutoStart(false).setExecutor(executor))) {
                assertNotNull(client);
            }

            assertFalse(executor.isShutdown());
        } finally {
            executor.shutdownNow();
        }
    }
}
