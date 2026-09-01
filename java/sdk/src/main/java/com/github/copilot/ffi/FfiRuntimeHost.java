/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.CopilotClientMode;
import com.github.copilot.rpc.CopilotClientOptions;

import com.sun.jna.Pointer;

/**
 * Manages the in-process FFI runtime lifecycle.
 */
public final class FfiRuntimeHost implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(FfiRuntimeHost.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final NativeBinding nativeBinding;
    private final QueueInputStream receiveStream;
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final AtomicInteger serverId = new AtomicInteger(0);
    private final AtomicInteger connectionId = new AtomicInteger(0);
    private final AtomicInteger activeCallbacks = new AtomicInteger(0);
    private final Object callbackDrainMonitor = new Object();
    private final ReentrantLock operationLock = new ReentrantLock();
    private final FfiOutputStream sendStream;
    private final String libraryPath;

    private volatile OutboundCallback callbackRef;

    /**
     * Creates an FFI runtime host using the resolved bundled native library.
     *
     * @throws IOException
     *             if the runtime library cannot be resolved
     */
    public FfiRuntimeHost() throws IOException {
        this(resolveLibraryPath(), null, new QueueInputStream());
    }

    FfiRuntimeHost(NativeBinding nativeBinding, String libraryPath) {
        this(nativeBinding, libraryPath, new QueueInputStream());
    }

    FfiRuntimeHost(NativeBinding nativeBinding, String libraryPath, QueueInputStream receiveStream) {
        this.nativeBinding = Objects.requireNonNull(nativeBinding, "nativeBinding must not be null");
        this.receiveStream = Objects.requireNonNull(receiveStream, "receiveStream must not be null");
        this.sendStream = new FfiOutputStream(this.nativeBinding, this.connectionId, this.closing, this.operationLock);
        this.libraryPath = libraryPath;
    }

    private FfiRuntimeHost(Path libraryPath, NativeBinding nativeBinding, QueueInputStream receiveStream) {
        this(nativeBinding == null ? new JnaNativeBinding(libraryPath) : nativeBinding, libraryPath.toString(),
                receiveStream);
    }

    private static Path resolveLibraryPath() throws IOException {
        return NativeRuntimeLoader.resolve();
    }

    /**
     * Starts the in-process runtime and opens a connection.
     *
     * @param entrypointPath
     *            optional explicit legacy CLI entrypoint passed in
     *            {@code argv_json}
     * @param options
     *            client options used to construct {@code argv_json} and
     *            {@code env_json}
     */
    public void start(String entrypointPath, CopilotClientOptions options) {
        Objects.requireNonNull(options, "options must not be null");
        if (disposed.get()) {
            throw new IllegalStateException("FfiRuntimeHost is already closed.");
        }
        if (serverId.get() != 0 || connectionId.get() != 0) {
            throw new IllegalStateException("FfiRuntimeHost has already been started.");
        }

        byte[] argvJson = buildArgvJson(entrypointPath, options);
        byte[] envJson = buildEnvJson(options);
        int hostHandle = runHostStartOnBlockingThread(argvJson, envJson);
        if (hostHandle == 0) {
            String lib = libraryPath != null ? libraryPath : "<unknown>";
            throw new IllegalStateException("copilot_runtime_host_start failed (library '" + lib + "').");
        }

        // Hold operationLock while publishing handles to serialize with close().
        // Recheck disposed in case close() ran while hostStart was blocking.
        operationLock.lock();
        try {
            if (disposed.get()) {
                try {
                    nativeBinding.hostShutdown(hostHandle);
                } catch (Throwable ignored) {
                    // Best effort
                }
                throw new IllegalStateException("FfiRuntimeHost was closed during startup.");
            }
            serverId.set(hostHandle);

            OutboundCallback callback = createOutboundCallback();
            callbackRef = callback;
            int connHandle = nativeBinding.connectionOpen(hostHandle, callback, Pointer.NULL, null, 0, null, 0, null,
                    0);
            if (connHandle == 0) {
                try {
                    nativeBinding.hostShutdown(hostHandle);
                } catch (Throwable ignored) {
                    // Best effort
                }
                serverId.set(0);
                callbackRef = null;
                throw new IllegalStateException("copilot_runtime_connection_open failed.");
            }
            connectionId.set(connHandle);
            LOG.fine(() -> "Started FFI runtime host. Library=" + libraryPath + ", serverId=" + hostHandle
                    + ", connectionId=" + connHandle);
        } finally {
            operationLock.unlock();
        }
    }

    public InputStream getReceiveStream() {
        return receiveStream;
    }

    public OutputStream getSendStream() {
        return sendStream;
    }

    @Override
    public void close() {
        if (!disposed.compareAndSet(false, true)) {
            return;
        }

        closing.set(true);

        operationLock.lock();
        try {
            int connHandle = connectionId.getAndSet(0);
            if (connHandle != 0) {
                try {
                    nativeBinding.connectionClose(connHandle);
                } catch (Throwable t) {
                    LOG.log(Level.FINE, "Failed to close FFI connection", t);
                }
            }
        } finally {
            operationLock.unlock();
        }

        drainActiveCallbacks();

        int hostHandle = serverId.getAndSet(0);
        if (hostHandle != 0) {
            try {
                nativeBinding.hostShutdown(hostHandle);
            } catch (Throwable t) {
                LOG.log(Level.FINE, "Failed to shut down FFI host", t);
            }
        }

        try {
            receiveStream.close();
        } catch (Throwable ignored) {
            // never throw from close
        }

        callbackRef = null;
    }

    private void drainActiveCallbacks() {
        while (activeCallbacks.get() > 0) {
            synchronized (callbackDrainMonitor) {
                if (activeCallbacks.get() == 0) {
                    return;
                }
                try {
                    callbackDrainMonitor.wait(10L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private OutboundCallback createOutboundCallback() {
        return (userData, data, len) -> {
            if (closing.get()) {
                return;
            }
            activeCallbacks.incrementAndGet();
            try {
                int length = len.intValue();
                if (closing.get() || data == null || length <= 0) {
                    return;
                }
                byte[] bytes = data.getByteArray(0, length);
                if (!closing.get()) {
                    receiveStream.enqueue(bytes);
                }
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "Exception in FFI outbound callback", t);
            } finally {
                if (activeCallbacks.decrementAndGet() == 0) {
                    synchronized (callbackDrainMonitor) {
                        callbackDrainMonitor.notifyAll();
                    }
                }
            }
        };
    }

    private int runHostStartOnBlockingThread(byte[] argvJson, byte[] envJson) {
        ReaderThreadFactory readerThreadFactory = new ReaderThreadFactory();
        ExecutorService executor = Executors
                .newSingleThreadExecutor(runnable -> readerThreadFactory.create(runnable, "copilot-ffi-host-start"));
        try {
            Future<Integer> future = executor.submit(() -> nativeBinding.hostStart(argvJson, argvJson.length, envJson,
                    envJson == null ? 0 : envJson.length));
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting in-process runtime host.", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Failed to start in-process runtime host.", cause);
        } finally {
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static byte[] buildArgvJson(String entrypointPath, CopilotClientOptions options) {
        List<String> argv = new ArrayList<>();
        if (entrypointPath != null) {
            if (entrypointPath.toLowerCase().endsWith(".js")) {
                argv.add("node");
            }
            argv.add(entrypointPath);
            argv.add("--embedded-host");
            argv.add("--no-auto-update");
        }

        String logLevel = options.getLogLevel();
        if (logLevel != null && !logLevel.isBlank()) {
            argv.add("--log-level");
            argv.add(logLevel);
        }

        String gitHubToken = options.getGitHubToken();
        if (gitHubToken != null && !gitHubToken.isEmpty()) {
            argv.add("--auth-token-env");
            argv.add("COPILOT_SDK_AUTH_TOKEN");
        }

        boolean useLoggedInUser = options.getUseLoggedInUser().orElse(gitHubToken == null || gitHubToken.isEmpty());
        if (!useLoggedInUser) {
            argv.add("--no-auto-login");
        }

        if (options.getSessionIdleTimeoutSeconds().isPresent()
                && options.getSessionIdleTimeoutSeconds().getAsInt() > 0) {
            argv.add("--session-idle-timeout");
            argv.add(String.valueOf(options.getSessionIdleTimeoutSeconds().getAsInt()));
        }

        if (options.isRemote()) {
            argv.add("--remote");
        }

        String[] cliArgs = options.getCliArgs();
        if (cliArgs != null && cliArgs.length > 0) {
            for (String arg : cliArgs) {
                if (arg != null && !arg.isBlank()) {
                    argv.add(arg);
                }
            }
        }

        return jsonBytes(argv);
    }

    private static byte[] buildEnvJson(CopilotClientOptions options) {
        Map<String, String> env = new LinkedHashMap<>();

        String token = options.getGitHubToken();
        if (token != null && !token.isEmpty()) {
            env.put("COPILOT_SDK_AUTH_TOKEN", token);
        }
        String copilotHome = options.getCopilotHome();
        if (copilotHome != null && !copilotHome.isEmpty()) {
            env.put("COPILOT_HOME", copilotHome);
        }
        if (options.getMode() == CopilotClientMode.EMPTY) {
            env.put("COPILOT_DISABLE_KEYTAR", "1");
        }

        if (env.isEmpty()) {
            return null;
        }
        return jsonBytes(env);
    }

    private static byte[] jsonBytes(Object value) {
        try {
            return MAPPER.writeValueAsString(value).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize FFI JSON parameter.", e);
        }
    }
}
