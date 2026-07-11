/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Diagnostic main launched as a separate JVM by
 * {@code InternalExecutorProviderIT} to inspect the multi-release behaviour of
 * {@link InternalExecutorProvider} against the actually packaged JAR.
 * <p>
 * Lives in the same package as {@link InternalExecutorProvider} so it can use
 * its package-private API directly, without reflection.
 * <p>
 * Output format (key=value, one per line):
 *
 * <pre>
 *   feature=&lt;JDK feature version&gt;
 *   canBeShutdown=&lt;true|false&gt;
 *   virtual=&lt;true|false&gt;
 * </pre>
 */
final class InternalExecutorProviderProbe {

    private InternalExecutorProviderProbe() {
    }

    public static void main(String[] args) throws Exception {
        InternalExecutorProvider provider = new InternalExecutorProvider(null);
        Executor executor = provider.get();
        boolean canBeShutdown = provider.canBeShutdown();

        AtomicBoolean virtual = new AtomicBoolean();
        CountDownLatch latch = new CountDownLatch(1);
        executor.execute(() -> {
            try {
                virtual.set(isCurrentThreadVirtual());
            } finally {
                latch.countDown();
            }
        });

        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                System.out.println("error=task-timeout");
                System.exit(2);
            }
        } finally {
            if (executor instanceof ExecutorService es) {
                es.shutdownNow();
            }
        }

        System.out.println("feature=" + Runtime.version().feature());
        System.out.println("canBeShutdown=" + canBeShutdown);
        System.out.println("virtual=" + virtual.get());
    }

    private static boolean isCurrentThreadVirtual() {
        try {
            Method isVirtual = Thread.class.getMethod("isVirtual");
            return (Boolean) isVirtual.invoke(Thread.currentThread());
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
