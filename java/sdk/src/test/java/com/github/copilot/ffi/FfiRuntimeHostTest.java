/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.CopilotClientMode;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.InternalTestOptions;
import com.github.copilot.rpc.TelemetryConfig;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

class FfiRuntimeHostTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private abstract static class TestBinding implements NativeBinding {
        @Override
        public boolean connectionSetUiThreadHandler(int connectionId, boolean enabled) {
            throw new UnsupportedOperationException("UI scheduling is not exercised by this test");
        }

        @Override
        public boolean connectionRunUiAction(int connectionId, int actionId) {
            throw new UnsupportedOperationException("UI scheduling is not exercised by this test");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void startBuildsExpectedArgvAndEnvJson(boolean captureContent) throws Exception {
        var before = Map.copyOf(System.getenv());
        class RecordingBinding extends TestBinding {
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
        options.setTelemetry(new TelemetryConfig().setOtlpEndpoint("http://collector:4318")
                .setOtlpProtocol("http/protobuf").setFilePath("/telemetry.jsonl").setExporterType("file")
                .setSourceName("inprocess-sdk").setCaptureContent(captureContent));

        options.setInternalTestOptions(
                new InternalTestOptions().setCopilotApiUrl("http://typed-replay").setGitHubApiUrl("http://typed-github")
                        .setProxyUrl("http://typed-proxy").setNoProxy("").setCaCertificatePath("/test/ca.pem"));
        FfiRuntimeHost host = new FfiRuntimeHost(binding, "/tmp/runtime.node");
        host.start("/tmp/entrypoint.js", "octocat/Hello-World", options);

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
        assertTrue(argv.contains("--cli-login-sync-integration-id=octocat/Hello-World"));

        Map<String, String> env = MAPPER.readValue(binding.env, new TypeReference<Map<String, String>>() {
        });
        if ("win32".equals(PlatformDetector.detectOs())) {
            assertEquals((long) env.size(),
                    env.keySet().stream().map(key -> key.toLowerCase(Locale.ROOT)).distinct().count(),
                    "The FFI payload must not contain case-colliding Windows environment names");
            var caseInsensitiveEnv = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            caseInsensitiveEnv.putAll(env);
            env = caseInsensitiveEnv;
        } else {
            assertEquals("http://typed-proxy", env.get("http_proxy"));
            assertEquals("http://typed-proxy", env.get("https_proxy"));
        }
        assertEquals("gh-token", env.get("COPILOT_SDK_AUTH_TOKEN"));
        assertEquals("/tmp/copilot-home", env.get("COPILOT_HOME"));
        assertEquals(java.nio.file.Path.of("/tmp/copilot-home", "cache").toString(), env.get("COPILOT_CACHE_HOME"));
        assertEquals("1", env.get("COPILOT_DISABLE_KEYTAR"));
        assertEquals("http://typed-replay", env.get("COPILOT_API_URL"));
        assertEquals("http://typed-github", env.get("COPILOT_DEBUG_GITHUB_API_URL"));
        assertEquals("http://typed-proxy", env.get("HTTP_PROXY"));
        assertEquals("http://typed-proxy", env.get("HTTPS_PROXY"));
        assertEquals("", env.get("NO_PROXY"));
        for (String key : List.of("NODE_EXTRA_CA_CERTS", "SSL_CERT_FILE", "REQUESTS_CA_BUNDLE", "CURL_CA_BUNDLE",
                "GIT_SSL_CAINFO")) {
            assertEquals("/test/ca.pem", env.get(key), key);
        }
        assertEquals(null, options.getEnvironment());
        assertEquals("true", env.get("COPILOT_OTEL_ENABLED"));
        assertEquals("http://collector:4318", env.get("OTEL_EXPORTER_OTLP_ENDPOINT"));
        assertEquals("http/protobuf", env.get("OTEL_EXPORTER_OTLP_PROTOCOL"));
        assertEquals("/telemetry.jsonl", env.get("COPILOT_OTEL_FILE_EXPORTER_PATH"));
        assertEquals("file", env.get("COPILOT_OTEL_EXPORTER_TYPE"));
        assertEquals("inprocess-sdk", env.get("COPILOT_OTEL_SOURCE_NAME"));
        assertEquals(captureContent ? "SPAN_ONLY" : "NO_CONTENT",
                env.get("OTEL_INSTRUMENTATION_GENAI_CAPTURE_CONTENT"));
        assertEquals(String.valueOf(captureContent), env.get("OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"));
        assertEquals(before, System.getenv());
        host.close();
    }

    @Test
    void startWithoutEntrypointPassesOnlyRuntimeOptions() throws Exception {
        AtomicReference<byte[]> argvJson = new AtomicReference<>();
        AtomicReference<byte[]> envJson = new AtomicReference<>();
        NativeBinding binding = new TestBinding() {
            @Override
            public int hostStart(byte[] argv, int argvLen, byte[] env, int envLen) {
                argvJson.set(argv);
                envJson.set(env);
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
        };

        try (FfiRuntimeHost host = new FfiRuntimeHost(binding, "/tmp/runtime.node")) {
            host.start(null, null, new CopilotClientOptions().setLogLevel("debug"));
        }

        List<String> argv = MAPPER.readValue(argvJson.get(), new TypeReference<List<String>>() {
        });
        assertEquals(List.of("--log-level", "debug"), argv);
        Map<String, String> expectedEnvironment = "win32".equals(PlatformDetector.detectOs())
                ? new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
                : new HashMap<>();
        expectedEnvironment.putAll(System.getenv());
        expectedEnvironment.remove("NODE_DEBUG");
        assertEquals(expectedEnvironment, MAPPER.readValue(envJson.get(), new TypeReference<Map<String, String>>() {
        }), "Without an explicit map, pass the effective inherited environment to the runtime");
    }

    @Test
    void callbackExceptionIsContainedAndDoesNotEscapeAcrossFfiBoundary() {
        AtomicBoolean callbackReturned = new AtomicBoolean(false);
        NativeBinding binding = new TestBinding() {
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
        assertDoesNotThrow(() -> host.start("/tmp/entrypoint", null, new CopilotClientOptions()));
        assertTrue(callbackReturned.get(), "callback should return normally even when enqueue throws");
    }

    @Test
    void closeNeverThrowsEvenWhenNativeCloseFails() {
        NativeBinding binding = new TestBinding() {
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
        host.start("/tmp/entrypoint", null, new CopilotClientOptions());
        assertDoesNotThrow(host::close);
    }

    @Test
    void failedConnectionOpenReleasesHostForSequentialStartup() {
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger shutdowns = new AtomicInteger();
        NativeBinding binding = new TestBinding() {
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

    @ParameterizedTest
    @CsvSource({"true, 11", "true, 0", "false, 11"})
    void abandonedStartupReleasesLateHost(boolean interrupt, int hostHandle) throws Exception {
        var nativeStarted = new CompletableFuture<Thread>();
        var nativeResult = new CompletableFuture<Integer>();
        AtomicBoolean interrupted = new AtomicBoolean();
        AtomicInteger shutdowns = new AtomicInteger();
        NativeBinding binding = mock(NativeBinding.class);
        when(binding.hostStart(any(), anyInt(), any(), anyInt())).thenAnswer(invocation -> {
            nativeStarted.complete(Thread.currentThread());
            // Native startup cannot be cancelled by interrupting its Java worker.
            return nativeResult.join();
        });
        when(binding.hostShutdown(anyInt())).thenAnswer(invocation -> {
            shutdowns.incrementAndGet();
            return true;
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        FfiRuntimeHost host = new FfiRuntimeHost(binding, "test-lib");
        try {
            var starting = executor.submit(() -> {
                try {
                    host.start(null, new CopilotClientOptions());
                } finally {
                    interrupted.set(Thread.currentThread().isInterrupted());
                }
            });
            Thread nativeWorker = nativeStarted.get(5, TimeUnit.SECONDS);
            if (interrupt) {
                executor.shutdownNow();
                var failure = assertThrows(ExecutionException.class, () -> starting.get(5, TimeUnit.SECONDS));
                assertInstanceOf(IllegalStateException.class, failure.getCause());
                assertInstanceOf(InterruptedException.class, failure.getCause().getCause());
                assertTrue(interrupted.get(), "startup must preserve the caller's interrupt status");
            } else {
                host.close();
            }

            nativeResult.complete(hostHandle);
            if (!interrupt) {
                var failure = assertThrows(ExecutionException.class, () -> starting.get(5, TimeUnit.SECONDS));
                assertEquals("FfiRuntimeHost was closed during startup.", failure.getCause().getMessage());
            }
            nativeWorker.join(5000);
            assertFalse(nativeWorker.isAlive(), "native startup and its cleanup must finish");
            assertEquals(hostHandle == 0 ? 0 : 1, shutdowns.get(), "abandoned startup must release its late host");
        } finally {
            nativeResult.complete(hostHandle);
            host.close();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "startup caller must terminate");
            Thread nativeWorker = nativeStarted.getNow(null);
            if (nativeWorker != null) {
                nativeWorker.join(5000);
                assertFalse(nativeWorker.isAlive(), "native startup worker must terminate");
            }
        }

        verify(binding).hostStart(any(), anyInt(), any(), anyInt());
        if (hostHandle != 0) {
            verify(binding).hostShutdown(hostHandle);
        }
        verifyNoMoreInteractions(binding);
    }

    @Test
    void writeAndCloseAreSerializedByOperationLock() throws Exception {
        CountDownLatch writeStarted = new CountDownLatch(1);
        CountDownLatch allowWriteToFinish = new CountDownLatch(1);
        AtomicInteger writes = new AtomicInteger(0);

        NativeBinding binding = new TestBinding() {
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
        host.start("/tmp/entrypoint", null, new CopilotClientOptions());

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
    void closeRetriesUntilCallbackQuiescenceIsReported() throws Exception {
        CountDownLatch callbackEntered = new CountDownLatch(1);
        CountDownLatch allowCallbackToReturn = new CountDownLatch(1);
        CountDownLatch shutdownCalled = new CountDownLatch(1);
        AtomicBoolean shutdownObservedAfterCallbackReturn = new AtomicBoolean(false);
        AtomicBoolean callbackFinished = new AtomicBoolean(false);
        AtomicInteger closeCalls = new AtomicInteger(0);
        AtomicInteger shutdownCalls = new AtomicInteger(0);
        AtomicReference<OutboundCallback> callbackRef = new AtomicReference<>();

        NativeBinding binding = new TestBinding() {
            @Override
            public int hostStart(byte[] argvJson, int argvJsonLen, byte[] envJson, int envJsonLen) {
                return 7;
            }

            @Override
            public boolean hostShutdown(int serverId) {
                shutdownCalls.incrementAndGet();
                shutdownObservedAfterCallbackReturn.set(callbackFinished.get());
                shutdownCalled.countDown();
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
                closeCalls.incrementAndGet();
                return callbackFinished.get();
            }
        };

        QueueInputStream blockingStream = new QueueInputStream() {
            @Override
            void enqueue(byte[] bytes) {
                callbackEntered.countDown();
                try {
                    allowCallbackToReturn.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                callbackFinished.set(true);
                super.enqueue(bytes);
            }
        };

        FfiRuntimeHost host = new FfiRuntimeHost(binding, "test-lib", blockingStream);
        host.start("/tmp/entrypoint", null, new CopilotClientOptions());
        assertNotNull(callbackRef.get());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<Void> callbackFuture = CompletableFuture.runAsync(() -> {
                Memory mem = new Memory(1);
                mem.setByte(0, (byte) 'x');
                callbackRef.get().invoke(Pointer.NULL, mem, new SizeT(1));
            }, executor);

            assertTrue(callbackEntered.await(2, TimeUnit.SECONDS));
            CompletableFuture<Void> closeFuture = CompletableFuture.runAsync(host::close, executor);
            closeFuture.get(5, TimeUnit.SECONDS);
            assertEquals(0, shutdownCalls.get(), "host shutdown must wait for a successful connection close");

            allowCallbackToReturn.countDown();
            callbackFuture.get(5, TimeUnit.SECONDS);
            assertTrue(shutdownCalled.await(5, TimeUnit.SECONDS), "deferred cleanup should retry connection close");
            assertTrue(closeCalls.get() >= 2, "connection close should be retried after reporting non-quiescence");
            assertTrue(shutdownObservedAfterCallbackReturn.get(), "host_shutdown should run after callback drains");
        } finally {
            allowCallbackToReturn.countDown();
            executor.shutdownNow();
        }

        host.close();
        assertEquals(1, shutdownCalls.get(), "host shutdown should happen exactly once");
    }
}
