package com.github.copilot.spike.mrjar;

import java.util.logging.Logger;

/**
 * Creates the reader thread used to consume data from the {@link QueueInputStream}.
 *
 * <p><strong>JDK 25 multi-release overlay</strong> — creates a virtual thread.
 * The virtual thread unmounts from its carrier while blocked on
 * {@code BlockingQueue.take()}, freeing the OS thread for other work.
 *
 * <p><strong>Multi-release JAR contract.</strong> This class is the JDK 25 sibling
 * of the baseline implementation at
 * {@code src/main/java/com/github/copilot/spike/mrjar/ReaderThreadFactory.java}.
 * The package-private surface ({@link #create(Runnable)}, {@link #name()}) is identical.
 */
final class ReaderThreadFactory {

    private static final Logger LOG = Logger.getLogger(ReaderThreadFactory.class.getName());

    Thread create(Runnable task) {
        LOG.info("[JDK-25] Creating VIRTUAL thread for queue reader");
        return Thread.ofVirtual()
                .name("queue-reader-virtual")
                .unstarted(task);
    }

    String name() {
        return "JDK-25/virtual-thread";
    }
}
