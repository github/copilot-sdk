/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.sun.jna.Pointer;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link JnaNativeBinding}.
 *
 * <p>
 * Delegation and callback-tracking tests use a stub
 * {@link JnaNativeBinding.CopilotRuntimeLibrary}. Library-loading guard tests
 * exercise {@link JnaNativeBinding} directly against the real
 * {@code runtime.node} when it is available on the test classpath.
 *
 * <p>
 * Tests that require the packaged native runtime are conditionally skipped when
 * it is unavailable (for example, when not running with {@code -Pinprocess}).
 */
class JnaNativeBindingTest {

    // -------------------------------------------------------------------------
    // Stub CopilotRuntimeLibrary for delegation tests
    // -------------------------------------------------------------------------

    /**
     * Minimal stub for testing {@link JnaNativeBinding} delegation without disk
     * I/O.
     */
    private static class StubRuntimeLibrary implements JnaNativeBinding.CopilotRuntimeLibrary {
        int hostStartReturn = 1;
        byte hostShutdownReturn = 1;
        int connectionOpenReturn = 1;
        byte connectionWriteReturn = 1;
        byte connectionCloseReturn = 1;

        byte[] lastArgvJson;
        int lastArgvJsonLen;
        int lastServerId;
        int lastConnectionId;
        OutboundCallback lastCallback;

        @Override
        public int copilot_runtime_host_start(byte[] argvJson, SizeT argvJsonLen, byte[] envJson, SizeT envJsonLen) {
            lastArgvJson = argvJson;
            lastArgvJsonLen = argvJsonLen.intValue();
            return hostStartReturn;
        }

        @Override
        public byte copilot_runtime_host_shutdown(int serverId) {
            lastServerId = serverId;
            return hostShutdownReturn;
        }

        @Override
        public int copilot_runtime_connection_open(int serverId, OutboundCallback callback, Pointer userData,
                byte[] extSource, SizeT extSourceLen, byte[] extName, SizeT extNameLen, byte[] connToken,
                SizeT connTokenLen) {
            lastServerId = serverId;
            lastCallback = callback;
            return connectionOpenReturn;
        }

        @Override
        public byte copilot_runtime_connection_write(int connectionId, byte[] data, SizeT dataLen) {
            lastConnectionId = connectionId;
            return connectionWriteReturn;
        }

        @Override
        public byte copilot_runtime_connection_close(int connectionId) {
            lastConnectionId = connectionId;
            return connectionCloseReturn;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Path resolveNativeLib() {
        try {
            return NativeRuntimeLoader.resolve();
        } catch (Exception e) {
            return null;
        }
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
        stub.hostShutdownReturn = 1;
        JnaNativeBinding binding = new JnaNativeBinding(stub);

        assertTrue(binding.hostShutdown(42));
        assertEquals(42, stub.lastServerId);
    }

    @Test
    void hostShutdownReturnsFalseOnFailure() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.hostShutdownReturn = 0;
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
        stub.connectionWriteReturn = 1;
        JnaNativeBinding binding = new JnaNativeBinding(stub);

        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        assertTrue(binding.connectionWrite(7, data, data.length));
        assertEquals(7, stub.lastConnectionId);
    }

    @Test
    void connectionWriteReturnsFalseOnFailure() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.connectionWriteReturn = 0;
        JnaNativeBinding binding = new JnaNativeBinding(stub);

        byte[] data = "x".getBytes(StandardCharsets.UTF_8);
        assertFalse(binding.connectionWrite(1, data, data.length),
                "connectionWrite must propagate false return from the library");
    }

    @Test
    void connectionCloseDelegatesToLibrary() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.connectionCloseReturn = 1;
        JnaNativeBinding binding = new JnaNativeBinding(stub);
        assertTrue(binding.connectionClose(7));
        assertEquals(7, stub.lastConnectionId);
    }

    @Test
    void connectionCloseReturnsFalseOnFailure() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.connectionCloseReturn = 0;
        JnaNativeBinding binding = new JnaNativeBinding(stub);
        assertFalse(binding.connectionClose(1));
    }

    @Test
    void activeCallbacksStartsAtZero() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        JnaNativeBinding binding = new JnaNativeBinding(stub);
        assertEquals(0, binding.activeCallbacks.get(), "Active callback counter must start at zero");
    }

    @Test
    void callbackWrapperRemainsReachableAfterConnectionClose() throws InterruptedException {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.connectionOpenReturn = 99;
        JnaNativeBinding binding = new JnaNativeBinding(stub);
        AtomicInteger invocations = new AtomicInteger();

        WeakReference<OutboundCallback> callbackReference = openAndCloseConnection(binding, stub,
                (userData, data, len) -> invocations.incrementAndGet());

        awaitGarbageCollection(callbackReference);

        OutboundCallback callback = callbackReference.get();
        assertNotNull(callback, "Callback wrapper must remain strongly reachable after connection close");
        callback.invoke(Pointer.NULL, Pointer.NULL, new SizeT(0));
        assertEquals(1, invocations.get(), "A callback queued before close must remain safely invocable");
    }

    private static WeakReference<OutboundCallback> openAndCloseConnection(JnaNativeBinding binding,
            StubRuntimeLibrary stub, OutboundCallback callback) {
        int connectionId = binding.connectionOpen(1, callback, Pointer.NULL, null, 0, null, 0, null, 0);
        assertEquals(99, connectionId);
        assertNotNull(stub.lastCallback);

        WeakReference<OutboundCallback> callbackReference = new WeakReference<>(stub.lastCallback);
        stub.lastCallback = null;
        assertTrue(binding.connectionClose(connectionId));
        return callbackReference;
    }

    private static void awaitGarbageCollection(WeakReference<?> reference) throws InterruptedException {
        for (int attempt = 0; attempt < 20 && reference.get() != null; attempt++) {
            System.gc();
            System.runFinalization();
            Thread.sleep(10);
        }
    }

    // =========================================================================
    // Duplicate-load guard
    // =========================================================================

    @Test
    void loadFromDifferentPathThrowsIllegalState(@TempDir Path tempDir) throws Exception {
        Path nativeLib = resolveNativeLib();
        assumeTrue(nativeLib != null, "Native runtime not available (run with -Pinprocess)");
        Path altPath = tempDir.resolve("runtime-copy-alt.node");
        Files.copy(nativeLib, altPath);

        new JnaNativeBinding(nativeLib);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new JnaNativeBinding(altPath));

        String msg = ex.getMessage();
        assertTrue(msg.contains("already loaded from"), "Diagnostic must mention 'already loaded from', got: " + msg);
        assertTrue(msg.contains(nativeLib.toString()), "Diagnostic must contain path A, got: " + msg);
        assertTrue(msg.contains(altPath.toString()), "Diagnostic must contain path B, got: " + msg);
    }

    @Test
    void duplicateLoadDiagnosticMentionsNotSupported(@TempDir Path tempDir) throws Exception {
        Path nativeLib = resolveNativeLib();
        assumeTrue(nativeLib != null, "Native runtime not available (run with -Pinprocess)");
        Path altPath = tempDir.resolve("runtime-copy-b.node");
        Files.copy(nativeLib, altPath);

        new JnaNativeBinding(nativeLib);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new JnaNativeBinding(altPath));
        assertTrue(ex.getMessage().contains("not supported"),
                "Diagnostic must mention 'not supported', got: " + ex.getMessage());
    }

    @Test
    void resetForTestingAllowsReloadFromDifferentPath() throws Exception {
        Path nativeLib = resolveNativeLib();
        assumeTrue(nativeLib != null, "Native runtime not available (run with -Pinprocess)");
        Path altDir = Path.of("target", "jna-test-runtime");
        Files.createDirectories(altDir);
        Path altPath = altDir.resolve("runtime-copy-reset.node");
        Files.copy(nativeLib, altPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        new JnaNativeBinding(nativeLib);

        JnaNativeBinding.resetForTesting();

        // After reset, a different path must succeed.
        new JnaNativeBinding(altPath);
    }

    @Test
    void activeCallbackCountIsIncrementedDuringCallback() {
        StubRuntimeLibrary stub = new StubRuntimeLibrary();
        stub.connectionOpenReturn = 99;
        JnaNativeBinding binding = new JnaNativeBinding(stub);

        AtomicInteger observedDuringCallback = new AtomicInteger(-1);

        OutboundCallback userCallback = (userData, data, len) -> {
            // Observe binding.activeCallbacks while inside the callback
            observedDuringCallback.set(binding.activeCallbacks.get());
        };

        binding.connectionOpen(1, userCallback, Pointer.NULL, null, 0, null, 0, null, 0);

        // The stub captured the tracked wrapper — invoke it to trigger tracking
        assertNotNull(stub.lastCallback, "Stub must have captured the tracked callback");
        stub.lastCallback.invoke(Pointer.NULL, Pointer.NULL, new SizeT(0));

        assertEquals(1, observedDuringCallback.get(), "binding.activeCallbacks must be 1 during callback execution");
        assertEquals(0, binding.activeCallbacks.get(),
                "binding.activeCallbacks must return to 0 after callback completes");
    }

}
