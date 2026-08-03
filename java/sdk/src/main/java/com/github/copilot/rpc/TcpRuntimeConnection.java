/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.ArrayList;
import java.util.List;

/**
 * Spawns a runtime child process listening on a TCP socket and connects to it.
 * Construct with {@link RuntimeConnection#forTcp()}.
 *
 * @since 1.0.0
 */
public final class TcpRuntimeConnection extends RuntimeConnection {

    private String path;
    private int port;
    private String connectionToken;
    private List<String> args;

    TcpRuntimeConnection() {
    }

    /**
     * Returns the path to the runtime executable.
     *
     * @return the path, or {@code null} to use the runtime discovered on the
     *         {@code PATH}
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path to the runtime executable.
     *
     * @param path
     *            the path, or {@code null} to use the runtime discovered on the
     *            {@code PATH}
     * @return this instance for method chaining
     */
    public TcpRuntimeConnection setPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * Returns the TCP port the spawned runtime listens on.
     *
     * @return the port, or {@code 0} to auto-allocate a free port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the TCP port the spawned runtime listens on.
     *
     * @param port
     *            the port, or {@code 0} (the default) to auto-allocate a free port
     * @return this instance for method chaining
     */
    public TcpRuntimeConnection setPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * Returns the shared secret the SDK sends to the spawned runtime to
     * authenticate the TCP connection.
     *
     * @return the token, or {@code null} to generate one automatically
     */
    public String getConnectionToken() {
        return connectionToken;
    }

    /**
     * Sets the shared secret the SDK sends to the spawned runtime to authenticate
     * the TCP connection.
     *
     * @param connectionToken
     *            the token, or {@code null} to generate one automatically
     * @return this instance for method chaining
     */
    public TcpRuntimeConnection setConnectionToken(String connectionToken) {
        this.connectionToken = connectionToken;
        return this;
    }

    /**
     * Returns the extra command-line arguments passed to the runtime process.
     *
     * @return the arguments, or {@code null} if none are configured
     */
    public List<String> getArgs() {
        return args;
    }

    /**
     * Sets extra command-line arguments passed to the runtime process.
     *
     * @param args
     *            the arguments, or {@code null} for none
     * @return this instance for method chaining
     */
    public TcpRuntimeConnection setArgs(List<String> args) {
        this.args = args == null ? null : new ArrayList<>(args);
        return this;
    }
}
