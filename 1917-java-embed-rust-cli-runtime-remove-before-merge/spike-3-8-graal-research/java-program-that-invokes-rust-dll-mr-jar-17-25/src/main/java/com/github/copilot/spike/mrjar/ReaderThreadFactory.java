package com.github.copilot.spike.mrjar;

import java.util.logging.Logger;

/**
 * Creates the reader thread used to consume data from the {@link QueueInputStream}.
 *
 * <p><strong>JDK 17 baseline</strong> — creates a platform (OS) thread.
 *
 * <p><strong>Multi-release JAR contract.</strong> This class has a sibling variant
 * at {@code src/main/java25/com/github/copilot/spike/mrjar/ReaderThreadFactory.java}
 * compiled with {@code --release 25} into {@code META-INF/versions/25/}. Both classes
 * expose the same package-private surface: {@link #create(Runnable)}, {@link #name()}.
 *
 * <p>On JDK 17: platform thread (pins an OS thread while blocked on queue.take()).
 * On JDK 25: virtual thread (unmounts from carrier while blocked, freeing the OS thread).
 */
final class ReaderThreadFactory {

    private static final Logger LOG = Logger.getLogger(ReaderThreadFactory.class.getName());

    Thread create(Runnable task) {
        LOG.info("[JDK-17] Creating PLATFORM thread for queue reader");
        Thread t = new Thread(task, "queue-reader-platform");
        t.setDaemon(true);
        return t;
    }

    String name() {
        return "JDK-17/platform-thread";
    }
}
