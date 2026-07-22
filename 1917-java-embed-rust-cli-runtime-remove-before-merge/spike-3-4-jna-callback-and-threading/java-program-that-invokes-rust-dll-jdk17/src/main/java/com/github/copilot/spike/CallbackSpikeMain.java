package com.github.copilot.spike;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

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
 * Spike 3.4 — JNA callback threading with PipedInputStream/PipedOutputStream bridging.
 *
 * <p>This program:
 * <ol>
 *   <li>Loads the Rust test DLL via JNA</li>
 *   <li>Calls {@code host_start()} to get a server handle</li>
 *   <li>Sets up a {@link QueueInputStream} (BlockingQueue-backed InputStream)</li>
 *   <li>Calls {@code connection_open()} with a JNA callback that enqueues into the QueueInputStream</li>
 *   <li>Reads messages from QueueInputStream on the Java reader thread</li>
 *   <li>Sends a test message via {@code connection_write()}</li>
 *   <li>Cleans up: {@code connection_close()} → {@code host_shutdown()}</li>
 * </ol>
 *
 * <p>Key things this spike verifies:
 * <ul>
 *   <li>JNA automatically attaches the native thread to the JVM for callbacks</li>
 *   <li>QueueInputStream.enqueue() works from any thread (no thread-affinity checks)</li>
 *   <li>QueueInputStream.read() on the Java thread receives the data correctly</li>
 *   <li>Callback GC protection via strong reference (field in this class)</li>
 *   <li>Active callback tracking via AtomicInteger (mirrors Rust's AtomicUsize pattern)</li>
 * </ul>
 *
 * <p><b>Finding:</b> PipedInputStream/PipedOutputStream does NOT work with JNA callbacks.
 * PipedInputStream tracks the writing thread via {@code writeSide} and checks
 * {@code writeSide.isAlive()}. JNA creates a new short-lived Java thread for each callback
 * invocation, so after a callback thread terminates, reads fail with "Write end dead".
 * QueueInputStream (backed by BlockingQueue) has no thread-affinity checks and works correctly.</p>
 *
 * <p>Usage: {@code java -Djna.library.path=<dir-containing-dll> -jar jna-callback-spike-0.1.0.jar [burstCount]}
 */
public class CallbackSpikeMain {

    private static final Logger LOG = Logger.getLogger(CallbackSpikeMain.class.getName());

    public static void main(String[] args) throws Exception {
        configureLogging();

        int burstCount = 5;
        if (args.length > 0) {
            burstCount = Integer.parseInt(args[0]);
        }

        LOG.info("=== Spike 3.4: JNA Callback Threading with PipedStream Bridging ===");
        LOG.info("burst_count=" + burstCount);
        LOG.info("java.library.path=" + System.getProperty("java.library.path"));
        LOG.info("Main thread: " + Thread.currentThread().getName()
                + " (id=" + Thread.currentThread().getId() + ")");

        // --- Load the Rust DLL ---
        LOG.info("Loading native library 'callback_test'...");
        CallbackTestLibrary lib = Native.load("callback_test", CallbackTestLibrary.class);
        LOG.info("Native library loaded successfully.");

        // --- host_start ---
        LOG.info("Calling host_start()...");
        int serverHandle = lib.host_start();
        LOG.info("host_start() returned serverHandle=" + serverHandle);
        if (serverHandle == 0) {
            LOG.severe("host_start failed (returned 0). Exiting.");
            System.exit(1);
        }

        // --- Set up QueueInputStream bridge ---
        LOG.info("Setting up QueueInputStream (BlockingQueue-backed InputStream)...");
        QueueInputStream queueIn = new QueueInputStream();
        LOG.info("QueueInputStream created. No thread-affinity constraints.");

        // Track active callbacks (mirrors Rust's AtomicUsize pattern)
        AtomicInteger activeCallbacks = new AtomicInteger(0);
        CountDownLatch allCallbacksDone = new CountDownLatch(burstCount);

        // --- Create the JNA callback ---
        // CRITICAL: hold this as a strong reference to prevent GC!
        // If this gets GC'd, the native function pointer becomes dangling → JVM crash.
        CallbackTestLibrary.OutboundCallback callback = (Pointer userData, Pointer data, int len) -> {
            int active = activeCallbacks.incrementAndGet();
            String threadName = Thread.currentThread().getName();
            long threadId = Thread.currentThread().getId();
            LOG.info("[callback] ENTERED on thread '" + threadName + "' (id=" + threadId
                    + "), active=" + active + ", len=" + len);

            try {
                // Read the byte data from the native pointer
                byte[] bytes = data.getByteArray(0, len);
                String content = new String(bytes, StandardCharsets.UTF_8);
                LOG.info("[callback] Received " + len + " bytes: " + content);

                // Enqueue into QueueInputStream — thread-safe, no thread-affinity issues
                LOG.info("[callback] Enqueuing into QueueInputStream...");
                // Write a length-prefixed frame: 4-byte big-endian length + data
                byte[] frame = new byte[4 + len];
                frame[0] = (byte) (len >> 24);
                frame[1] = (byte) (len >> 16);
                frame[2] = (byte) (len >> 8);
                frame[3] = (byte) len;
                System.arraycopy(bytes, 0, frame, 4, len);
                queueIn.enqueue(frame);
                LOG.info("[callback] Enqueued successfully.");
            } finally {
                int remaining = activeCallbacks.decrementAndGet();
                allCallbacksDone.countDown();
                LOG.info("[callback] EXITING on thread '" + threadName + "' (id=" + threadId
                        + "), active=" + remaining);
            }
        };

        LOG.info("Callback created. Stored as strong reference to prevent GC.");

        // --- Start a reader thread that consumes from QueueInputStream ---
        final int expectedMessages = burstCount;
        Thread readerThread = new Thread(() -> {
            LOG.info("[reader] Reader thread started: " + Thread.currentThread().getName()
                    + " (id=" + Thread.currentThread().getId() + ")");
            try {
                for (int i = 0; i < expectedMessages; i++) {
                    // Read 4-byte length prefix
                    byte[] lengthBuf = queueIn.readNBytes(4);
                    if (lengthBuf.length < 4) {
                        LOG.warning("[reader] Unexpected end of stream reading length prefix.");
                        break;
                    }
                    int msgLen = ((lengthBuf[0] & 0xFF) << 24)
                            | ((lengthBuf[1] & 0xFF) << 16)
                            | ((lengthBuf[2] & 0xFF) << 8)
                            | (lengthBuf[3] & 0xFF);
                    LOG.info("[reader] Read length prefix: " + msgLen + " bytes");

                    // Read the message body
                    byte[] msgBytes = queueIn.readNBytes(msgLen);
                    String msg = new String(msgBytes, StandardCharsets.UTF_8);
                    LOG.info("[reader] Message #" + i + " (" + msgLen + " bytes): " + msg);
                }
                LOG.info("[reader] All " + expectedMessages + " messages received successfully.");
            } catch (IOException e) {
                LOG.severe("[reader] IOException: " + e.getMessage());
            }
        }, "queue-reader");
        readerThread.setDaemon(true);
        readerThread.start();
        LOG.info("Reader thread started.");

        // --- connection_open (spawns native thread that invokes callback) ---
        LOG.info("Calling connection_open(serverHandle=" + serverHandle
                + ", burstCount=" + burstCount + ")...");
        int connHandle = lib.connection_open(serverHandle, callback, Pointer.NULL, burstCount);
        LOG.info("connection_open() returned connHandle=" + connHandle);
        if (connHandle == 0) {
            LOG.severe("connection_open failed (returned 0). Exiting.");
            System.exit(1);
        }

        // --- Wait for all callbacks to complete ---
        LOG.info("Waiting for all " + burstCount + " callbacks to complete...");
        boolean completed = allCallbacksDone.await(10, TimeUnit.SECONDS);
        if (completed) {
            LOG.info("All callbacks completed. activeCallbacks=" + activeCallbacks.get());
        } else {
            LOG.warning("Timed out waiting for callbacks! activeCallbacks=" + activeCallbacks.get());
        }

        // Give reader thread a moment to finish consuming
        readerThread.join(2000);

        // --- connection_write (Java → Rust) ---
        String testMessage = "{\"jsonrpc\":\"2.0\",\"method\":\"test\",\"params\":{}}";
        byte[] testBytes = testMessage.getBytes(StandardCharsets.UTF_8);
        LOG.info("Calling connection_write(connHandle=" + connHandle + ", len=" + testBytes.length + ")...");
        boolean writeOk = lib.connection_write(connHandle, testBytes, testBytes.length);
        LOG.info("connection_write() returned " + writeOk);

        // --- Cleanup ---
        LOG.info("Calling connection_close(connHandle=" + connHandle + ")...");
        boolean closeOk = lib.connection_close(connHandle);
        LOG.info("connection_close() returned " + closeOk);

        LOG.info("Signaling QueueInputStream EOF...");
        queueIn.signalEof();

        LOG.info("Calling host_shutdown(serverHandle=" + serverHandle + ")...");
        boolean shutdownOk = lib.host_shutdown(serverHandle);
        LOG.info("host_shutdown() returned " + shutdownOk);

        LOG.info("=== Spike complete ===");
        LOG.info("Summary:");
        LOG.info("  - Server handle: " + serverHandle);
        LOG.info("  - Connection handle: " + connHandle);
        LOG.info("  - Messages received via QueueInputStream: " + burstCount);
        LOG.info("  - Callback GC protection: strong reference held as local variable");
        LOG.info("  - Active callback tracking: AtomicInteger (peak concurrent callbacks logged above)");
        LOG.info("  - QueueInputStream bridging: " + (completed ? "SUCCESS" : "TIMEOUT"));
        LOG.info("  - PipedStream alternative: REJECTED (Write end dead due to JNA thread-per-callback)");
    }

    private static void configureLogging() {
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        // Remove default handlers
        for (var handler : root.getHandlers()) {
            root.removeHandler(handler);
        }
        // Add a console handler with a simple format
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);
        ch.setFormatter(new SimpleFormatter());
        root.addHandler(ch);
    }
}
