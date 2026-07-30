/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

/**
 * Resolves the {@link Executor} used by {@link CopilotClient} for internal
 * asynchronous work.
 *
 * <p>This is the <strong>JDK 25+ multi-release variant</strong>. It is
 * compiled with {@code --release 25} into
 * {@code META-INF/versions/25/com/github/copilot/InternalExecutorProvider.class}
 * inside the packaged JAR and is automatically loaded in preference to the
 * baseline class when the JVM runtime feature version is 25 or greater.
 * When no user-provided executor is supplied, it creates an SDK-owned
 * {@link Executors#newVirtualThreadPerTaskExecutor() virtual-thread executor}
 * that is shut down by {@link CopilotClient#close()}.
 *
 * <p><strong>Multi-release JAR contract.</strong> This class is the
 * JDK 25 sibling of the baseline implementation at
 * {@code src/main/java/com/github/copilot/InternalExecutorProvider.java}.
 * The package-private surface of both classes
 * ({@link #InternalExecutorProvider(Executor) constructor},
 * {@link #get()}, {@link #canBeShutdown()}) <strong>must be kept in
 * lock-step</strong>; only the default-executor strategy and ownership
 * semantics differ.
 *
 * @implNote
 * Maintainers: when editing this file, also edit
 * {@code src/main/java/com/github/copilot/InternalExecutorProvider.java}.
 * The packaged JAR is verified at build time (see the
 * {@code java25-multi-release} profile in {@code pom.xml}) to ensure this
 * overlay class is present.
 */
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
