/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Map;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Configuration options for creating a
 * {@link com.github.copilot.sdk.CopilotClient}.
 * <p>
 * This class provides a fluent API for configuring how the client connects to
 * and manages the Copilot CLI server. All setter methods return {@code this}
 * for method chaining.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var options = new CopilotClientOptions().setCliPath("/usr/local/bin/copilot").setLogLevel("debug")
 * 		.setAutoStart(true);
 *
 * var client = new CopilotClient(options);
 * }</pre>
 *
 * @see com.github.copilot.sdk.CopilotClient
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CopilotClientOptions {

    private String cliPath;
    private String[] cliArgs;
    private String cwd;
    private int port;
    private boolean useStdio = true;
    private String cliUrl;
    private String logLevel = "info";
    private boolean autoStart = true;
    private boolean autoRestart = true;
    private Map<String, String> environment;
    private Logger logger;
    private String githubToken;
    private Boolean useLoggedInUser;

    /**
     * Gets the path to the Copilot CLI executable.
     *
     * @return the CLI path, or {@code null} to use "copilot" from PATH
     */
    public String getCliPath() {
        return cliPath;
    }

    /**
     * Sets the path to the Copilot CLI executable.
     *
     * @param cliPath
     *            the path to the CLI executable, or {@code null} to use "copilot"
     *            from PATH
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setCliPath(String cliPath) {
        this.cliPath = cliPath;
        return this;
    }

    /**
     * Gets the extra CLI arguments.
     *
     * @return the extra arguments to pass to the CLI
     */
    public String[] getCliArgs() {
        return cliArgs;
    }

    /**
     * Sets extra arguments to pass to the CLI process.
     * <p>
     * These arguments are prepended before SDK-managed flags.
     *
     * @param cliArgs
     *            the extra arguments to pass
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setCliArgs(String[] cliArgs) {
        this.cliArgs = cliArgs;
        return this;
    }

    /**
     * Gets the working directory for the CLI process.
     *
     * @return the working directory path
     */
    public String getCwd() {
        return cwd;
    }

    /**
     * Sets the working directory for the CLI process.
     *
     * @param cwd
     *            the working directory path
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setCwd(String cwd) {
        this.cwd = cwd;
        return this;
    }

    /**
     * Gets the TCP port for the CLI server.
     *
     * @return the port number, or 0 for a random port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the TCP port for the CLI server to listen on.
     * <p>
     * This is only used when {@link #isUseStdio()} is {@code false}.
     *
     * @param port
     *            the port number, or 0 for a random port
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * Returns whether to use stdio transport instead of TCP.
     *
     * @return {@code true} to use stdio (default), {@code false} to use TCP
     */
    public boolean isUseStdio() {
        return useStdio;
    }

    /**
     * Sets whether to use stdio transport instead of TCP.
     * <p>
     * Stdio transport is more efficient and is the default. TCP transport can be
     * useful for debugging or connecting to remote servers.
     *
     * @param useStdio
     *            {@code true} to use stdio, {@code false} to use TCP
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setUseStdio(boolean useStdio) {
        this.useStdio = useStdio;
        return this;
    }

    /**
     * Gets the URL of an existing CLI server to connect to.
     *
     * @return the CLI server URL, or {@code null} to spawn a new process
     */
    public String getCliUrl() {
        return cliUrl;
    }

    /**
     * Sets the URL of an existing CLI server to connect to.
     * <p>
     * When provided, the client will not spawn a CLI process but will connect to
     * the specified URL instead. Format: "host:port" or "http://host:port".
     * <p>
     * <strong>Note:</strong> This is mutually exclusive with
     * {@link #setUseStdio(boolean)} and {@link #setCliPath(String)}.
     *
     * @param cliUrl
     *            the CLI server URL to connect to
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setCliUrl(String cliUrl) {
        this.cliUrl = cliUrl;
        return this;
    }

    /**
     * Gets the log level for the CLI process.
     *
     * @return the log level (default: "info")
     */
    public String getLogLevel() {
        return logLevel;
    }

    /**
     * Sets the log level for the CLI process.
     * <p>
     * Valid levels include: "error", "warn", "info", "debug", "trace".
     *
     * @param logLevel
     *            the log level
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setLogLevel(String logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    /**
     * Returns whether the client should automatically start the server.
     *
     * @return {@code true} to auto-start (default), {@code false} for manual start
     */
    public boolean isAutoStart() {
        return autoStart;
    }

    /**
     * Sets whether the client should automatically start the CLI server when the
     * first request is made.
     *
     * @param autoStart
     *            {@code true} to auto-start, {@code false} for manual start
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
        return this;
    }

    /**
     * Returns whether the client should automatically restart the server on crash.
     *
     * @return {@code true} to auto-restart (default), {@code false} otherwise
     */
    public boolean isAutoRestart() {
        return autoRestart;
    }

    /**
     * Sets whether the client should automatically restart the CLI server if it
     * crashes unexpectedly.
     *
     * @param autoRestart
     *            {@code true} to auto-restart, {@code false} otherwise
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
        return this;
    }

    /**
     * Gets the environment variables for the CLI process.
     *
     * @return the environment variables map
     */
    public Map<String, String> getEnvironment() {
        return environment;
    }

    /**
     * Sets environment variables to pass to the CLI process.
     * <p>
     * When set, these environment variables replace the inherited environment.
     *
     * @param environment
     *            the environment variables map
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setEnvironment(Map<String, String> environment) {
        this.environment = environment;
        return this;
    }

    /**
     * Gets the custom logger for the client.
     *
     * @return the logger instance
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Sets a custom logger for the client.
     *
     * @param logger
     *            the logger instance to use
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Gets the GitHub token for authentication.
     *
     * @return the GitHub token, or {@code null} to use other authentication methods
     */
    public String getGithubToken() {
        return githubToken;
    }

    /**
     * Sets the GitHub token to use for authentication.
     * <p>
     * When provided, the token is passed to the CLI server via environment
     * variable. This takes priority over other authentication methods.
     *
     * @param githubToken
     *            the GitHub token
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setGithubToken(String githubToken) {
        this.githubToken = githubToken;
        return this;
    }

    /**
     * Returns whether to use the logged-in user for authentication.
     *
     * @return {@code true} to use logged-in user auth, {@code false} to use only
     *         explicit tokens, or {@code null} to use default behavior
     */
    public Boolean getUseLoggedInUser() {
        return useLoggedInUser;
    }

    /**
     * Sets whether to use the logged-in user for authentication.
     * <p>
     * When true, the CLI server will attempt to use stored OAuth tokens or gh CLI
     * auth. When false, only explicit tokens (githubToken or environment variables)
     * are used. Default: true (but defaults to false when githubToken is provided).
     *
     * @param useLoggedInUser
     *            {@code true} to use logged-in user auth, {@code false} otherwise
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setUseLoggedInUser(Boolean useLoggedInUser) {
        this.useLoggedInUser = useLoggedInUser;
        return this;
    }
}
