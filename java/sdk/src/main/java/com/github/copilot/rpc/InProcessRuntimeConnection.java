/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.github.copilot.CopilotExperimental;

/**
 * Hosts the runtime in-process by loading its native library and communicating
 * over the C ABI — no child process is spawned by the SDK for JSON-RPC
 * transport. Construct with {@link RuntimeConnection#forInProcess()}.
 * <p>
 * The in-process runtime is self-contained: it carries everything it needs and
 * requires no external installation. Process-scoped settings such as
 * {@code workingDirectory} and {@code environment} are configured on
 * out-of-process connections
 * ({@link StdioRuntimeConnection}/{@link TcpRuntimeConnection}), not on
 * {@link CopilotClientOptions}, because they do not apply to in-process (FFI)
 * hosting. Other per-client process settings such as {@code telemetry} and
 * {@code cliArgs} are also rejected here; configure those on the host process
 * instead, or use a child-process connection.
 *
 * @since 1.0.0
 */
@CopilotExperimental
public final class InProcessRuntimeConnection extends RuntimeConnection {

    InProcessRuntimeConnection() {
    }
}
