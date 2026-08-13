/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.github.copilot.CopilotExperimental;

/**
 * Configures how a {@link com.github.copilot.CopilotClient} connects to the
 * Copilot runtime.
 * <p>
 * Instances are created through the factory methods on this class and assigned
 * with {@link CopilotClientOptions#setConnection(RuntimeConnection)}:
 *
 * <pre>{@code
 * // Spawn a runtime child process and talk over stdin/stdout (the default).
 * new CopilotClientOptions().setConnection(RuntimeConnection.forStdio());
 *
 * // Spawn a runtime child process listening on a TCP socket.
 * new CopilotClientOptions().setConnection(RuntimeConnection.forTcp().setPath("/usr/local/bin/copilot"));
 *
 * // Connect to an already-running runtime.
 * new CopilotClientOptions().setConnection(RuntimeConnection.forUri("localhost:3000"));
 * }</pre>
 *
 * @since 1.0.0
 */
@CopilotExperimental
public abstract sealed class RuntimeConnection
        permits StdioRuntimeConnection, TcpRuntimeConnection, UriRuntimeConnection, InProcessRuntimeConnection {

    RuntimeConnection() {
    }

    /**
     * Spawns a runtime child process and communicates over its stdin/stdout. This
     * is the default when no connection is configured.
     *
     * @return a new stdio connection
     */
    public static StdioRuntimeConnection forStdio() {
        return new StdioRuntimeConnection();
    }

    /**
     * Spawns a runtime child process at the given path and communicates over its
     * stdin/stdout.
     *
     * @param path
     *            path to the runtime executable, or {@code null} to use the runtime
     *            discovered on the {@code PATH}
     * @return a new stdio connection
     */
    public static StdioRuntimeConnection forStdio(String path) {
        return new StdioRuntimeConnection().setPath(path);
    }

    /**
     * Spawns a runtime child process that listens on a TCP socket and connects to
     * it.
     *
     * @return a new TCP connection
     */
    public static TcpRuntimeConnection forTcp() {
        return new TcpRuntimeConnection();
    }

    /**
     * Connects to an already-running runtime at the given URL.
     *
     * @param url
     *            URL of the runtime to connect to; accepts {@code "port"},
     *            {@code "host:port"}, or a full URL
     * @return a new URI connection
     * @throws IllegalArgumentException
     *             if {@code url} is {@code null} or empty
     */
    public static UriRuntimeConnection forUri(String url) {
        return new UriRuntimeConnection(url);
    }

    /**
     * Hosts the runtime in-process by loading its native library and communicating
     * over the C ABI — no child process is spawned by the SDK for JSON-RPC
     * transport.
     *
     * @return a new in-process connection
     */
    @CopilotExperimental
    public static InProcessRuntimeConnection forInProcess() {
        return new InProcessRuntimeConnection();
    }
}
