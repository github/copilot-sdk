/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link JnaNativeBinding} using the spike-3-4 test native
 * library ({@code libcallback_test}).
 *
 * <p>
 * The spike test library exports simplified versions of the runtime ABI
 * functions: {@code host_start}, {@code host_shutdown},
 * {@code connection_open}, {@code connection_write}, and
 * {@code connection_close}. Callback tests use this library through a
 * test-specific JNA interface, while loading and guard tests exercise
 * {@link JnaNativeBinding} directly.
 *
 * <p>
 * Tests that require the native library are conditionally skipped when the
 * library is not present (e.g. on an architecture without a pre-built binary).
 */
class JnaNativeBindingTest {

    /**
     * System property that points at the absolute path of the test native library.
     *
     * <p>
     * Default: the {@code libcallback_test.so} built from the spike-3-4 Rust crate,
     * relative to {@code java/sdk/}.
     */
    private static final String TEST_LIB_PATH_PROP = "copilot.test.nativelib.path";

    private static final String SPIKE_LIB_PATH = System.getProperty(TEST_LIB_PATH_PROP,
            "../../1917-java-embed-rust-cli-runtime-remove-before-merge" + "/spike-3-4-jna-callback-and-threading"
                    + "/rust-dll/target/release/libcallback_test.so");

    // -------------------------------------------------------------------------
    // Test-specific JNA interface for the spike-3-4 test library
    // -------------------------------------------------------------------------

    /**
     * JNA interface for the simplified test library. Maps Java names to the
     * snake_case exports of {@code libcallback_test}.
     */
    interface CallbackTestLib extends Library {
        /** Simulates {@code copilot_runtime_host_start}; always returns 42. */
        int host_start();

        /**
         * Simulates {@code copilot_runtime_host_shutdown}; always returns {@code true}.
         */
        boolean host_shutdown(int serverHandle);

        /**
         * Simulates {@code copilot_runtime_connection_open}. Spawns a native thread
         * that invokes {@code callback} {@code burstCount} times. Returns 7.
         */
        int connection_open(int serverHandle, OutboundCallback callback, Pointer userData, int burstCount);

        /**
         * Simulates {@code copilot_runtime_connection_write}; always returns
         * {@code true}.
         */
        boolean connection_write(int connectionHandle, byte[] data, int len);

        /**
         * Simulates {@code copilot_runtime_connection_close}; always returns
         * {@code true}.
         */
        boolean connection_close(int connectionHandle);
    }

    // -------------------------------------------------------------------------
    // Stub CopilotRuntimeLibrary for delegation tests
    // -------------------------------------------------------------------------

    /**
     * Minimal stub for testing {@link JnaNativeBinding} delegation without disk
     * I/O.
     */
    private static class StubRuntimeLibrary implements JnaNativeBinding.CopilotRuntimeLibrary {
        int hostStartReturn = 1;
        boolean hostShutdownReturn = true;
        int connectionOpenReturn = 1;
        boolean connectionWriteReturn = true;
        boolean connectionCloseReturn = true;

        byte[] lastArgvJson;
        int lastArgvJsonLen;
        int lastServerId;
        int lastConnectionId;

        @Override
        public int copilot_runtime_host_start(byte[] argvJson, int argvJsonLen, byte[] envJson, int envJsonLen) {
            lastArgvJson = argvJson;
            lastArgvJsonLen = argvJsonLen;
            return hostStartReturn;
        }

        @Override
        public boolean copilot_runtime_host_shutdown(int serverId) {
            lastServerId = serverId;
            return hostShutdownReturn;
        }

        @Override
        public int copilot_runtime_connection_open(int serverId, OutboundCallback callback, Pointer userData,
                byte[] extSource, int extSourceLen, byte[] extName, int extNameLen, byte[] connToken,
                int connTokenLen) {
            lastServerId = serverId;
            return connectionOpenReturn;
        }

        @Override
        public boolean copilot_runtime_connection_write(int connectionId, byte[] data, int dataLen) {
            lastConnectionId = connectionId;
            return connectionWriteReturn;
        }

        @Override
        public boolean copilot_runtime_connection_close(int connectionId) {
            lastConnectionId = connectionId;
            return connectionCloseReturn;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean testLibExists() {
        return Files.isRegularFile(testLibAbsPath());
    }

    private static Path testLibAbsPath() {
        return Path.of(SPIKE_LIB_PATH).toAbsolutePath().normalize();
    }

    private static CallbackTestLib loadTestLib() {
        return Native.load(testLibAbsPath().toString(), CallbackTestLib.class);
    }

    @AfterEach
    void resetStaticState() {
        JnaNativeBinding.resetForTesting();
    }

    // =========================================================================
    // Delegation via testing constructor (stub — no disk I/O)
    // =========================================================================

    @Test
    void hostStartDelegatesToLibraryAndReturnsHandle() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.hostStartReturn = 77;
        JnaNativeBinding binding = new JnaNativeBinding(stub);

        byte[] argv = "[\"copilot\"]".getBytes(StandardCharsets.UTF_8);
        int result = binding.hostStart(argv, argv.length, null, 0);

        assertEquals(77, result, "hostStart should return the stub's configured value");
        assertEquals(argv, stub.lastArgvJson, "argv bytes should be passed through unchanged");
        assertEquals(argv.length, stub.lastArgvJsonLen);
    }

    @Test
    void hostStartReturnsZeroOnFailure() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.hostStartReturn = 0;
        JnaNativeBinding binding = new JnaNativeBinding(stub);

        byte[] argv = "[\"copilot\"]".getBytes(StandardCharsets.UTF_8);
        assertEquals(0, binding.hostStart(argv, argv.length, null, 0), "hostStart must return 0 to signal failure");
    }

    @Test
    void hostShutdownDelegatesToLibrary() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.hostShutdownReturn = true;
        JnaNativeBinding binding = new JnaNativeBinding(stub);

        assertTrue(binding.hostShutdown(42));
        assertEquals(42, stub.lastServerId);
    }

    @Test
    void hostShutdownReturnsFalseOnFailure() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.hostShutdownReturn = false;
        JnaNativeBinding binding = new JnaNativeBinding(stub);
        assertFalse(binding.hostShutdown(1));
    }

    @Test
    void connectionOpenDelegatesToLibrary() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.connectionOpenReturn = 55;
        JnaNativeBinding binding = new JnaNativeBinding(stub);

        OutboundCallback noop = (ud, data, len) -> {
        };
        int connId = binding.connectionOpen(42, noop, Pointer.NULL, null, 0, null, 0, null, 0);

        assertEquals(55, connId, "connectionOpen should return the stub's configured handle");
        assertEquals(42, stub.lastServerId);
    }

    @Test
    void connectionOpenReturnsZeroOnFailure() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.connectionOpenReturn = 0;
        JnaNativeBinding binding = new JnaNativeBinding(stub);

        OutboundCallback noop = (ud, data, len) -> {
        };
        assertEquals(0, binding.connectionOpen(1, noop, Pointer.NULL, null, 0, null, 0, null, 0),
                "connectionOpen must return 0 to signal failure");
    }

    @Test
    void connectionWriteDelegatesToLibrary() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.connectionWriteReturn = true;
        JnaNativeBinding binding = new JnaNativeBinding(stub);

        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        assertTrue(binding.connectionWrite(7, data, data.length));
        assertEquals(7, stub.lastConnectionId);
    }

    @Test
    void connectionWriteReturnsFalseOnFailure() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.connectionWriteReturn = false;
        JnaNativeBinding binding = new JnaNativeBinding(stub);

        byte[] data = "x".getBytes(StandardCharsets.UTF_8);
        assertFalse(binding.connectionWrite(1, data, data.length),
                "connectionWrite must propagate false return from the library");
    }

    @Test
    void connectionCloseDelegatesToLibrary() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.connectionCloseReturn = true;
        JnaNativeBinding binding = new JnaNativeBinding(stub);
        assertTrue(binding.connectionClose(7));
        assertEquals(7, stub.lastConnectionId);
    }

    @Test
    void connectionCloseReturnsFalseOnFailure() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.connectionCloseReturn = false;
        JnaNativeBinding binding = new JnaNativeBinding(stub);
        assertFalse(binding.connectionClose(1));
    }

    @Test
    void activeCallbacksStartsAtZero() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        JnaNativeBinding binding = new JnaNativeBinding(stub);
        assertEquals(0, binding.activeCallbacks.get(), "Active callback counter must start at zero");
    }

    // =========================================================================
    // Library loading — success paths (requires native library on disk)
    // =========================================================================

    @Test
    void loadByPathSucceedsWhenLibraryExists() {
        if (!testLibExists()) {
            return;
        }
        JnaNativeBinding binding = new JnaNativeBinding(testLibAbsPath());
        assertNotNull(binding);
    }

    @Test
    void loadByPathTwiceWithSamePathSucceeds() {
        if (!testLibExists()) {
            return;
        }
        new JnaNativeBinding(testLibAbsPath());
        // Second construction with the same absolute path must not throw.
        new JnaNativeBinding(testLibAbsPath());
    }

    @Test
    void activeCallbacksStartsAtZeroAfterPathLoad() {
        if (!testLibExists()) {
            return;
        }
        JnaNativeBinding binding = new JnaNativeBinding(testLibAbsPath());
        assertEquals(0, binding.activeCallbacks.get());
    }

    // =========================================================================
    // Duplicate-load guard
    // =========================================================================

    @Test
    void loadFromDifferentPathThrowsIllegalState(@TempDir Path tempDir) throws Exception {
        if (!testLibExists()) {
            return;
        }
        Path altPath = tempDir.resolve("libcallback_test_alt.so");
        Files.copy(testLibAbsPath(), altPath);

        new JnaNativeBinding(testLibAbsPath());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new JnaNativeBinding(altPath));

        String msg = ex.getMessage();
        assertTrue(msg.contains("already loaded from"), "Diagnostic must mention 'already loaded from', got: " + msg);
        assertTrue(msg.contains(testLibAbsPath().toString()), "Diagnostic must contain path A, got: " + msg);
        assertTrue(msg.contains(altPath.toString()), "Diagnostic must contain path B, got: " + msg);
    }

    @Test
    void duplicateLoadDiagnosticMentionsNotSupported(@TempDir Path tempDir) throws Exception {
        if (!testLibExists()) {
            return;
        }
        Path altPath = tempDir.resolve("libcallback_test_b.so");
        Files.copy(testLibAbsPath(), altPath);

        new JnaNativeBinding(testLibAbsPath());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new JnaNativeBinding(altPath));
        assertTrue(ex.getMessage().contains("not supported"),
                "Diagnostic must mention 'not supported', got: " + ex.getMessage());
    }

    @Test
    void resetForTestingAllowsReloadFromDifferentPath(@TempDir Path tempDir) throws Exception {
        if (!testLibExists()) {
            return;
        }
        Path altPath = tempDir.resolve("libcallback_test_reset.so");
        Files.copy(testLibAbsPath(), altPath);

        new JnaNativeBinding(testLibAbsPath());

        JnaNativeBinding.resetForTesting();

        // After reset, a different path must succeed.
        new JnaNativeBinding(altPath);
    }

    // =========================================================================
    // Callback invocation via test native library
    // =========================================================================

    @Test
    void callbackIsInvokedFromNativeThread() throws Exception {
        if (!testLibExists()) {
            return;
        }
        CallbackTestLib lib = loadTestLib();
        int serverHandle = lib.host_start();
        assertEquals(42, serverHandle, "host_start should return 42");

        int burstCount = 3;
        CountDownLatch latch = new CountDownLatch(burstCount);
        AtomicInteger callbackCount = new AtomicInteger(0);
        AtomicInteger activeCallbacks = new AtomicInteger(0);

        OutboundCallback callback = (userData, data, len) -> {
            activeCallbacks.incrementAndGet();
            try {
                callbackCount.incrementAndGet();
                // Copy before returning — pointer only valid during invocation.
                byte[] bytes = data.getByteArray(0, len);
                assertEquals(len, bytes.length, "Copied byte array length must equal len parameter");
            } finally {
                activeCallbacks.decrementAndGet();
                latch.countDown();
            }
        };

        int connHandle = lib.connection_open(serverHandle, callback, Pointer.NULL, burstCount);
        assertEquals(7, connHandle, "connection_open should return 7");

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All callbacks must complete within 10 seconds");
        assertEquals(burstCount, callbackCount.get(), "Callback must be invoked exactly burstCount times");
        assertEquals(0, activeCallbacks.get(),
                "Active callback count must return to zero after all callbacks complete");
    }

    @Test
    void activeCallbackCountIsIncrementedDuringCallback() throws Exception {
        if (!testLibExists()) {
            return;
        }
        CallbackTestLib lib = loadTestLib();
        int serverHandle = lib.host_start();

        int burstCount = 1;
        CountDownLatch enteredLatch = new CountDownLatch(burstCount);
        CountDownLatch exitLatch = new CountDownLatch(burstCount);
        AtomicInteger observedOnEntry = new AtomicInteger(-1);
        AtomicInteger observedOnExit = new AtomicInteger(-1);
        AtomicInteger activeCallbacks = new AtomicInteger(0);

        OutboundCallback callback = (userData, data, len) -> {
            observedOnEntry.set(activeCallbacks.incrementAndGet());
            enteredLatch.countDown();
            try {
                data.getByteArray(0, len); // copy as required
            } finally {
                observedOnExit.set(activeCallbacks.decrementAndGet());
                exitLatch.countDown();
            }
        };

        lib.connection_open(serverHandle, callback, Pointer.NULL, burstCount);

        assertTrue(enteredLatch.await(10, TimeUnit.SECONDS), "Callback must be entered within 10 seconds");
        assertEquals(1, observedOnEntry.get(), "Active count must be 1 while callback is executing");

        assertTrue(exitLatch.await(10, TimeUnit.SECONDS), "Callback must exit within 10 seconds");
        assertEquals(0, observedOnExit.get(), "Active count must return to 0 after callback exits");
    }

    @Test
    void callbackDataContainsJsonRpcContent() throws Exception {
        if (!testLibExists()) {
            return;
        }
        CallbackTestLib lib = loadTestLib();
        int serverHandle = lib.host_start();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        OutboundCallback callback = (userData, data, len) -> {
            try {
                byte[] bytes = data.getByteArray(0, len);
                receivedMessage.set(new String(bytes, StandardCharsets.UTF_8));
            } finally {
                latch.countDown();
            }
        };

        lib.connection_open(serverHandle, callback, Pointer.NULL, 1);

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Callback must complete within 10 seconds");
        String msg = receivedMessage.get();
        assertNotNull(msg, "Received message must not be null");
        assertTrue(msg.contains("jsonrpc"), "Callback data should contain JSON-RPC content, got: " + msg);
    }

    @Test
    void multipleCallbacksDoNotLeakActiveCount() throws Exception {
        if (!testLibExists()) {
            return;
        }
        CallbackTestLib lib = loadTestLib();
        int serverHandle = lib.host_start();

        int burstCount = 5;
        CountDownLatch latch = new CountDownLatch(burstCount);
        AtomicInteger activeCallbacks = new AtomicInteger(0);
        AtomicInteger maxObservedActive = new AtomicInteger(0);

        OutboundCallback callback = (userData, data, len) -> {
            int current = activeCallbacks.incrementAndGet();
            maxObservedActive.updateAndGet(prev -> Math.max(prev, current));
            try {
                data.getByteArray(0, len);
            } finally {
                activeCallbacks.decrementAndGet();
                latch.countDown();
            }
        };

        lib.connection_open(serverHandle, callback, Pointer.NULL, burstCount);

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All callbacks must complete within timeout");
        assertEquals(0, activeCallbacks.get(), "Active count must be 0 after all callbacks complete");
        assertTrue(maxObservedActive.get() >= 1, "At least one callback must have been observed as active");
    }

    @Test
    void connectionWriteReturnsTrueForValidHandle() {
        if (!testLibExists()) {
            return;
        }
        CallbackTestLib lib = loadTestLib();
        int serverHandle = lib.host_start();
        int connHandle = lib.connection_open(serverHandle, (ud, data, len) -> {
        }, Pointer.NULL, 0);

        byte[] payload = "{\"jsonrpc\":\"2.0\",\"method\":\"ping\"}".getBytes(StandardCharsets.UTF_8);
        assertTrue(lib.connection_write(connHandle, payload, payload.length),
                "connection_write should return true for valid data");
    }

    @Test
    void connectionCloseReturnsTrueForValidHandle() {
        if (!testLibExists()) {
            return;
        }
        CallbackTestLib lib = loadTestLib();
        int serverHandle = lib.host_start();
        int connHandle = lib.connection_open(serverHandle, (ud, data, len) -> {
        }, Pointer.NULL, 0);

        assertTrue(lib.connection_close(connHandle), "connection_close should return true");
    }

    @Test
    void hostShutdownReturnsTrueForValidHandle() {
        if (!testLibExists()) {
            return;
        }
        CallbackTestLib lib = loadTestLib();
        int serverHandle = lib.host_start();
        assertTrue(lib.host_shutdown(serverHandle), "host_shutdown should return true");
    }
}
