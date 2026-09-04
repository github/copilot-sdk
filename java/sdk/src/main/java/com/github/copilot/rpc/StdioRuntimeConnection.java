/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.copilot.CopilotExperimental;

/**
 * Spawns a runtime child process and communicates over its stdin/stdout.
 * Construct with {@link RuntimeConnection#forStdio()} or
 * {@link RuntimeConnection#forStdio(String)}.
 * <p>
 * Process-scoped settings are configured on this out-of-process connection, not
 * on {@link CopilotClientOptions}, because they do not apply to in-process
 * (FFI) hosting.
 *
 * @since 1.0.0
 */
@CopilotExperimental
public final class StdioRuntimeConnection extends RuntimeConnection {

    private String path;
    private String workingDirectory;
    private List<String> args;
    private Map<String, String> environment;

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
     * Returns the working directory for the spawned runtime process.
     *
     * @return the working directory path, or {@code null} to inherit the current
     *         process working directory
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Sets the working directory for the spawned runtime process.
     *
     * @param workingDirectory
     *            the working directory path, or {@code null} to inherit the current
     *            process working directory
     * @return this instance for method chaining
     */
    public StdioRuntimeConnection setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
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

    /**
     * Returns the environment variables for the spawned runtime process.
     * <p>
     * Returns a shallow copy of the internal map, or {@code null} if no environment
     * has been set.
     *
     * @return a copy of the environment variables map, or {@code null}
     */
    public Map<String, String> getEnvironment() {
        return environment != null ? new HashMap<>(environment) : null;
    }

    /**
     * Sets environment variables to pass to the spawned runtime process.
     * <p>
     * When set, these environment variables replace the inherited environment. A
     * shallow copy of the provided map is stored. If {@code null} or empty, the
     * existing environment is cleared.
     *
     * @param environment
     *            the environment variables map, or {@code null}/empty to clear
     * @return this instance for method chaining
     */
    public StdioRuntimeConnection setEnvironment(Map<String, String> environment) {
        if (environment == null || environment.isEmpty()) {
            if (this.environment != null) {
                this.environment.clear();
            }
        } else {
            this.environment = new HashMap<>(environment);
        }
        return this;
    }
}
