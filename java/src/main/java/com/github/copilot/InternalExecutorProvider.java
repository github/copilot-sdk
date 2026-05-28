/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Resolves the {@link Executor} used by {@link CopilotClient} for internal
 * asynchronous work.
 *
 * <p>
 * This is the <strong>baseline (JDK 17+) implementation</strong>. When no
 * user-provided executor is supplied, it falls back to
 * {@link ForkJoinPool#commonPool()}, which is shared with the rest of the JVM
 * and therefore never owned by the SDK.
 *
 * <p>
 * <strong>Multi-release JAR contract.</strong> This class has a sibling variant
 * at {@code src/main/java25/com/github/copilot/InternalExecutorProvider.java}
 * that is compiled with {@code --release 25} into {@code META-INF/versions/25/}
 * and selected automatically by the JVM on JDK 25+. Any change to the
 * package-private surface of this class
 * ({@link #InternalExecutorProvider(Executor) constructor}, {@link #get()},
 * {@link #canBeShutdown()}) <strong>must be mirrored in both source
 * trees</strong>. The two implementations must remain behaviourally
 * interchangeable from the caller's perspective; only the default-executor
 * strategy and ownership semantics differ.
 *
 * @implNote Maintainers: when editing this file, also edit
 *           {@code src/main/java25/com/github/copilot/InternalExecutorProvider.java}.
 *           The packaged JAR is verified at build time (see the
 *           {@code java25-multi-release} profile in {@code pom.xml}) to ensure
 *           the JDK 25 overlay is present.
 */
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
