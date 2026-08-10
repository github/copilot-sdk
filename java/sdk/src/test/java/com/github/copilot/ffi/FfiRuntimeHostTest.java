/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.CopilotClientMode;
import com.github.copilot.rpc.CopilotClientOptions;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

class FfiRuntimeHostTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void startBuildsExpectedArgvAndEnvJson() throws Exception {
        class RecordingBinding implements NativeBinding {
            byte[] argv;
            byte[] env;

            @Override
            public int hostStart(byte[] argvJson, int argvJsonLen, byte[] envJson, int envJsonLen) {
                this.argv = argvJson;
                this.env = envJson;
                return 11;
            }

            @Override
            public boolean hostShutdown(int serverId) {
                return true;
            }

            @Override
            public int connectionOpen(int serverId, OutboundCallback callback, Pointer userData, byte[] extSource,
                    int extSourceLen, byte[] extName, int extNameLen, byte[] connToken, int connTokenLen) {
                return 21;
            }

            @Override
            public boolean connectionWrite(int connectionId, byte[] data, int dataLen) {
                return true;
            }

            @Override
            public boolean connectionClose(int connectionId) {
                return true;
            }
        }

        RecordingBinding binding = new RecordingBinding();
        CopilotClientOptions options = new CopilotClientOptions().setLogLevel("debug").setGitHubToken("gh-token")
                .setCopilotHome("/tmp/copilot-home").setUseLoggedInUser(false).setSessionIdleTimeoutSeconds(42)
                .setRemote(true).setMode(CopilotClientMode.EMPTY).setCliArgs(new String[]{"--extra-flag"});

        FfiRuntimeHost host = new FfiRuntimeHost(binding, "/tmp/runtime.node");
        host.start("/tmp/entrypoint.js", options);

        List<String> argv = MAPPER.readValue(binding.argv, new TypeReference<List<String>>() {
        });
        assertEquals("node", argv.get(0));
        assertEquals("/tmp/entrypoint.js", argv.get(1));
        assertTrue(argv.contains("--embedded-host"));
        assertTrue(argv.contains("--no-auto-update"));
        assertTrue(argv.contains("--auth-token-env"));
        assertTrue(argv.contains("COPILOT_SDK_AUTH_TOKEN"));
        assertTrue(argv.contains("--no-auto-login"));
        assertTrue(argv.contains("--session-idle-timeout"));
        assertTrue(argv.contains("42"));
        assertTrue(argv.contains("--remote"));
        assertTrue(argv.contains("--extra-flag"));

        Map<String, String> env = MAPPER.readValue(binding.env, new TypeReference<Map<String, String>>() {
        });
        assertEquals("gh-token", env.get("COPILOT_SDK_AUTH_TOKEN"));
        assertEquals("/tmp/copilot-home", env.get("COPILOT_HOME"));
        assertEquals("1", env.get("COPILOT_DISABLE_KEYTAR"));
    }

    @Test
    void callbackExceptionIsContainedAndDoesNotEscapeAcrossFfiBoundary() {
        AtomicBoolean callbackReturned = new AtomicBoolean(false);
        NativeBinding binding = new NativeBinding() {
            @Override
            public int hostStart(byte[] argvJson, int argvJsonLen, byte[] envJson, int envJsonLen) {
                return 1;
            }

            @Override
            public boolean hostShutdown(int serverId) {
                return true;
            }

            @Override
            public int connectionOpen(int serverId, OutboundCallback callback, Pointer userData, byte[] extSource,
                    int extSourceLen, byte[] extName, int extNameLen, byte[] connToken, int connTokenLen) {
                Memory mem = new Memory(5);
                mem.write(0, "hello".getBytes(StandardCharsets.UTF_8), 0, 5);
                callback.invoke(Pointer.NULL, mem, new SizeT(5));
                callbackReturned.set(true);
                return 2;
            }

            @Override
            public boolean connectionWrite(int connectionId, byte[] data, int dataLen) {
                return true;
            }

            @Override
            public boolean connectionClose(int connectionId) {
                return true;
            }
        };

        QueueInputStream throwingStream = new QueueInputStream() {
            @Override
            void enqueue(byte[] bytes) {
                throw new RuntimeException("boom");
            }
        };

        FfiRuntimeHost host = new FfiRuntimeHost(binding, "test-lib", throwingStream);
        assertDoesNotThrow(() -> host.start("/tmp/entrypoint", new CopilotClientOptions()));
        assertTrue(callbackReturned.get(), "callback should return normally even when enqueue throws");
    }

    @Test
    void closeNeverThrowsEvenWhenNativeCloseFails() {
        NativeBinding binding = new NativeBinding() {
            @Override
            public int hostStart(byte[] argvJson, int argvJsonLen, byte[] envJson, int envJsonLen) {
                return 5;
            }

            @Override
            public boolean hostShutdown(int serverId) {
                throw new RuntimeException("shutdown failed");
            }

            @Override
            public int connectionOpen(int serverId, OutboundCallback callback, Pointer userData, byte[] extSource,
                    int extSourceLen, byte[] extName, int extNameLen, byte[] connToken, int connTokenLen) {
                return 9;
            }

            @Override
            public boolean connectionWrite(int connectionId, byte[] data, int dataLen) {
                return true;
            }

            @Override
            public boolean connectionClose(int connectionId) {
                throw new RuntimeException("close failed");
            }
        };

        FfiRuntimeHost host = new FfiRuntimeHost(binding, "test-lib");
        host.start("/tmp/entrypoint", new CopilotClientOptions());
        assertDoesNotThrow(host::close);
    }

    @Test
    void failedConnectionOpenReleasesHostForSequentialStartup() {
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger shutdowns = new AtomicInteger();
        NativeBinding binding = new NativeBinding() {
            @Override
            public int hostStart(byte[] argvJson, int argvJsonLen, byte[] envJson, int envJsonLen) {
                return starts.incrementAndGet();
            }

            @Override
            public boolean hostShutdown(int serverId) {
                shutdowns.incrementAndGet();
                return true;
            }

            @Override
            public int connectionOpen(int serverId, OutboundCallback callback, Pointer userData, byte[] extSource,
                    int extSourceLen, byte[] extName, int extNameLen, byte[] connToken, int connTokenLen) {
                return serverId == 1 ? 0 : 22;
            }

            @Override
            public boolean connectionWrite(int connectionId, byte[] data, int dataLen) {
                return true;
            }

            @Override
            public boolean connectionClose(int connectionId) {
                return true;
            }
        };

        try (FfiRuntimeHost failedHost = new FfiRuntimeHost(binding, "test-lib")) {
            assertThrows(IllegalStateException.class,
                    () -> failedHost.start("/tmp/entrypoint", new CopilotClientOptions()));
        }
        assertEquals(1, shutdowns.get(), "failed connection startup must release its native host");

        try (FfiRuntimeHost nextHost = new FfiRuntimeHost(binding, "test-lib")) {
            assertDoesNotThrow(() -> nextHost.start("/tmp/entrypoint", new CopilotClientOptions()));
        }
        assertEquals(2, shutdowns.get(), "the sequential host must also shut down cleanly");
    }

    @Test
    void writeAndCloseAreSerializedByOperationLock() throws Exception {
        CountDownLatch writeStarted = new CountDownLatch(1);
        CountDownLatch allowWriteToFinish = new CountDownLatch(1);
        AtomicInteger writes = new AtomicInteger(0);

        NativeBinding binding = new NativeBinding() {
            @Override
            public int hostStart(byte[] argvJson, int argvJsonLen, byte[] envJson, int envJsonLen) {
                return 3;
            }

            @Override
            public boolean hostShutdown(int serverId) {
                return true;
            }

            @Override
            public int connectionOpen(int serverId, OutboundCallback callback, Pointer userData, byte[] extSource,
                    int extSourceLen, byte[] extName, int extNameLen, byte[] connToken, int connTokenLen) {
                return 4;
            }

            @Override
            public boolean connectionWrite(int connectionId, byte[] data, int dataLen) {
                writes.incrementAndGet();
                writeStarted.countDown();
                try {
                    allowWriteToFinish.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }

            @Override
            public boolean connectionClose(int connectionId) {
                return true;
            }
        };

        FfiRuntimeHost host = new FfiRuntimeHost(binding, "test-lib");
        host.start("/tmp/entrypoint", new CopilotClientOptions());

        CompletableFuture<Void> writer = CompletableFuture.runAsync(() -> {
            try {
                host.getSendStream().write("ping".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(writeStarted.await(2, TimeUnit.SECONDS));
        CompletableFuture<Void> closer = CompletableFuture.runAsync(host::close);
        allowWriteToFinish.countDown();

        writer.get(5, TimeUnit.SECONDS);
        closer.get(5, TimeUnit.SECONDS);
        assertEquals(1, writes.get());
        assertThrows(IOException.class, () -> host.getSendStream().write("late".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void closeDrainsActiveCallbacksBeforeHostShutdown() throws Exception {
        CountDownLatch callbackEntered = new CountDownLatch(1);
        CountDownLatch allowCallbackToReturn = new CountDownLatch(1);
        AtomicBoolean shutdownObservedAfterCallbackReturn = new AtomicBoolean(false);
        AtomicBoolean callbackFinished = new AtomicBoolean(false);
        AtomicReference<OutboundCallback> callbackRef = new AtomicReference<>();

        NativeBinding binding = new NativeBinding() {
            @Override
            public int hostStart(byte[] argvJson, int argvJsonLen, byte[] envJson, int envJsonLen) {
                return 7;
            }

            @Override
            public boolean hostShutdown(int serverId) {
                shutdownObservedAfterCallbackReturn.set(callbackFinished.get());
                return true;
            }

            @Override
            public int connectionOpen(int serverId, OutboundCallback callback, Pointer userData, byte[] extSource,
                    int extSourceLen, byte[] extName, int extNameLen, byte[] connToken, int connTokenLen) {
                callbackRef.set(callback);
                return 8;
            }

            @Override
            public boolean connectionWrite(int connectionId, byte[] data, int dataLen) {
                return true;
            }

            @Override
            public boolean connectionClose(int connectionId) {
                return true;
            }
        };

        QueueInputStream blockingStream = new QueueInputStream() {
            @Override
            void enqueue(byte[] bytes) {
                callbackEntered.countDown();
                try {
                    allowCallbackToReturn.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                callbackFinished.set(true);
                super.enqueue(bytes);
            }
        };

        FfiRuntimeHost host = new FfiRuntimeHost(binding, "test-lib", blockingStream);
        host.start("/tmp/entrypoint", new CopilotClientOptions());
        assertNotNull(callbackRef.get());

        CompletableFuture<Void> callbackFuture = CompletableFuture.runAsync(() -> {
            Memory mem = new Memory(1);
            mem.setByte(0, (byte) 'x');
            callbackRef.get().invoke(Pointer.NULL, mem, new SizeT(1));
        });

        assertTrue(callbackEntered.await(2, TimeUnit.SECONDS));
        CompletableFuture<Void> closeFuture = CompletableFuture.runAsync(host::close);
        Thread.sleep(150);
        assertFalse(closeFuture.isDone(), "close should wait for active callback to drain");
        allowCallbackToReturn.countDown();
        callbackFuture.get(5, TimeUnit.SECONDS);
        closeFuture.get(5, TimeUnit.SECONDS);
        assertTrue(shutdownObservedAfterCallbackReturn.get(), "host_shutdown should run after callback drains");
    }
}
