/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Hosts the runtime in-process by loading its native library and communicating
 * over the C ABI — no child process is spawned by the SDK for JSON-RPC
 * transport. Construct with {@link RuntimeConnection#forInProcess()}.
 * <p>
 * The in-process runtime is self-contained: it carries everything it needs and
 * requires no external installation. Because it runs inside the host process,
 * per-client process settings ({@code environment}, {@code telemetry},
 * {@code cwd}, and {@code cliArgs}) are rejected; configure those on the host
 * process instead, or use a child-process connection.
 *
 * @since 1.0.0
 */
@CopilotExperimental
public final class InProcessRuntimeConnection extends RuntimeConnection {

    private Function<Runnable, CompletableFuture<Void>> runOnUiThread;

    InProcessRuntimeConnection() {
    }

    /**
     * Returns the experimental native UI scheduler.
     *
     * @return the scheduler, or {@code null} when not configured
     */
    public Function<Runnable, CompletableFuture<Void>> getRunOnUiThread() {
        return runOnUiThread;
    }

    /**
     * Schedules the supplied native action exactly once on the UI thread (the
     * process main thread on macOS), completing only after it returns. The action
     * can block while native UI is open. No authentication data crosses this
     * callback. Other in-process clients may share this process-wide capability.
     *
     * Required for OneAuth interactive authentication on macOS. This
     * general-purpose scheduler may also be used for other runtime-owned native UI
     * operations. Temporary OneAuth workaround; this API may be removed if no
     * longer needed. See
     * https://github.com/github/copilot-agent-runtime/issues/27032.
     *
     * @param handler
     *            the scheduler, or {@code null} to leave it unconfigured
     * @return this connection
     */
    public InProcessRuntimeConnection setRunOnUiThread(Function<Runnable, CompletableFuture<Void>> handler) {
        runOnUiThread = handler;
        return this;
    }
}
