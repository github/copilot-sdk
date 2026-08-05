package com.github.copilot.spike.mrjar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Spike 3.4 — Multi-Release JAR: JNA + platform thread (JDK 17) vs JNA + virtual thread (JDK 25).
 *
 * <p>Both JDK versions use JNA for the native binding (FFM deferred per ADR-007).
 * The MR-JAR swap point is {@link ReaderThreadFactory}:
 * <ul>
 *   <li><strong>JDK 17</strong>: platform thread for the queue reader — pins an OS
 *       thread while blocked on {@code queue.take()}.</li>
 *   <li><strong>JDK 25</strong>: virtual thread for the queue reader — unmounts from
 *       its carrier while blocked, freeing the OS thread for other work.</li>
 * </ul>
 *
 * <p>Both paths use {@link QueueInputStream} to bridge callback data into an
 * {@code InputStream} (no thread-affinity issues).
 *
 * <p>Usage (same for both JDK versions):
 * <pre>
 * java -Djna.library.path=../rust-dll/target/debug -jar jna-callback-mrjar-spike-0.1.0.jar [burstCount]
 * </pre>
 */
public class CallbackMrJarMain {

    private static final Logger LOG = Logger.getLogger(CallbackMrJarMain.class.getName());

    public static void main(String[] args) throws Exception {
        configureLogging();

        int burstCount = 5;
        if (args.length > 0) {
            burstCount = Integer.parseInt(args[0]);
        }

        // --- Instantiate provider (JNA on all JDK versions) and thread factory (MR-JAR swap) ---
        NativeBindingProvider provider = new NativeBindingProvider();
        ReaderThreadFactory threadFactory = new ReaderThreadFactory();
        String threadKind = threadFactory.name();

        LOG.info("=== Spike 3.4 MR-JAR: JNA + " + threadKind + " ===");
        LOG.info("JVM version: " + System.getProperty("java.version"));
        LOG.info("JVM vendor: " + System.getProperty("java.vendor"));
        LOG.info("Reader thread: " + threadKind);
        LOG.info("burst_count=" + burstCount);
        LOG.info("Main thread: " + Thread.currentThread().getName()
                + " (id=" + Thread.currentThread().getId() + ")");

        // --- Load the native library (JNA on all JDK versions) ---
        provider.loadLibrary("callback_test");

        // --- host_start ---
        int serverHandle = provider.hostStart();
        if (serverHandle == 0) {
            LOG.severe("host_start failed. Exiting.");
            System.exit(1);
        }

        // --- Set up QueueInputStream + callback tracking ---
        QueueInputStream queueIn = new QueueInputStream();
        AtomicInteger activeCallbacks = new AtomicInteger(0);
        CountDownLatch allCallbacksDone = new CountDownLatch(burstCount);

        // Wrap the QueueInputStream to also count down the latch
        QueueInputStream countingQueue = new QueueInputStream() {
            @Override
            public void enqueue(byte[] data) {
                queueIn.enqueue(data);
                allCallbacksDone.countDown();
            }

            @Override
            public void signalEof() {
                queueIn.signalEof();
            }
        };

        LOG.info("QueueInputStream created. No thread-affinity constraints.");

        // --- Start reader thread (MR-JAR selects platform vs virtual) ---
        final int expectedMessages = burstCount;
        Thread readerThread = threadFactory.create(() -> {
            LOG.info("[reader] Reader thread started: " + Thread.currentThread().getName()
                    + " (id=" + Thread.currentThread().getId() + ")");
            try {
                for (int i = 0; i < expectedMessages; i++) {
                    byte[] lengthBuf = queueIn.readNBytes(4);
                    if (lengthBuf.length < 4) {
                        LOG.warning("[reader] Unexpected end of stream.");
                        break;
                    }
                    int msgLen = ((lengthBuf[0] & 0xFF) << 24)
                            | ((lengthBuf[1] & 0xFF) << 16)
                            | ((lengthBuf[2] & 0xFF) << 8)
                            | (lengthBuf[3] & 0xFF);
                    LOG.info("[reader] Read length prefix: " + msgLen + " bytes");

                    byte[] msgBytes = queueIn.readNBytes(msgLen);
                    String msg = new String(msgBytes, StandardCharsets.UTF_8);
                    LOG.info("[reader] Message #" + i + " (" + msgLen + " bytes): " + msg);
                }
                LOG.info("[reader] All " + expectedMessages + " messages received successfully.");
            } catch (IOException e) {
                LOG.severe("[reader] IOException: " + e.getMessage());
            }
        });
        readerThread.start();
        LOG.info("Reader thread started: " + readerThread.getName()
                + " (kind=" + threadKind + ")");

        // --- connection_open ---
        int connHandle = provider.connectionOpen(serverHandle, countingQueue, activeCallbacks, burstCount);
        if (connHandle == 0) {
            LOG.severe("connection_open failed. Exiting.");
            System.exit(1);
        }

        // --- Wait for all callbacks ---
        LOG.info("Waiting for all " + burstCount + " callbacks...");
        boolean completed = allCallbacksDone.await(10, TimeUnit.SECONDS);
        if (completed) {
            LOG.info("All callbacks completed. activeCallbacks=" + activeCallbacks.get());
        } else {
            LOG.warning("Timed out! activeCallbacks=" + activeCallbacks.get());
        }

        readerThread.join(2000);

        // --- connection_write (Java → Rust) ---
        String testMessage = "{\"jsonrpc\":\"2.0\",\"method\":\"test\",\"params\":{}}";
        provider.connectionWrite(connHandle, testMessage.getBytes(StandardCharsets.UTF_8));

        // --- Cleanup ---
        provider.connectionClose(connHandle);
        queueIn.signalEof();
        provider.hostShutdown(serverHandle);

        LOG.info("=== Spike complete (JNA + " + threadKind + ") ===");
        LOG.info("Summary:");
        LOG.info("  - Native binding: JNA (all JDK versions)");
        LOG.info("  - Reader thread: " + threadKind);
        LOG.info("  - JVM: " + System.getProperty("java.version"));
        LOG.info("  - Server handle: " + serverHandle);
        LOG.info("  - Connection handle: " + connHandle);
        LOG.info("  - Messages received: " + burstCount);
        LOG.info("  - QueueInputStream bridging: " + (completed ? "SUCCESS" : "TIMEOUT"));
        LOG.info("  - Callback threads: new Java thread per invocation (JNA behavior on all JDKs)");
    }

    private static void configureLogging() {
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        for (var handler : root.getHandlers()) {
            root.removeHandler(handler);
        }
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);
        ch.setFormatter(new SimpleFormatter());
        root.addHandler(ch);
    }
}
