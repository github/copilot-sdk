/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.ArrayList;
import java.util.List;

import com.github.copilot.CopilotExperimental;

/**
 * Spawns a runtime child process and communicates over its stdin/stdout.
 * Construct with {@link RuntimeConnection#forStdio()} or
 * {@link RuntimeConnection#forStdio(String)}.
 *
 * @since 1.0.0
 */
@CopilotExperimental
public final class StdioRuntimeConnection extends RuntimeConnection {

    private String path;
    private List<String> args;

    StdioRuntimeConnection() {
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
    public StdioRuntimeConnection setPath(String path) {
        this.path = path;
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
    public StdioRuntimeConnection setArgs(List<String> args) {
        this.args = args == null ? null : new ArrayList<>(args);
        return this;
    }
}
