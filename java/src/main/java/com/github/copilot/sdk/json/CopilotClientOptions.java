/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

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

    @Deprecated
    private boolean autoRestart;
    private boolean autoStart = true;
    private String[] cliArgs;
    private String cliPath;
    private String cliUrl;
    private String cwd;
    private Map<String, String> environment;
    private Executor executor;
    private String gitHubToken;
    private String logLevel = "info";
    private Supplier<CompletableFuture<List<ModelInfo>>> onListModels;
    private int port;
    private TelemetryConfig telemetry;
    private Boolean useLoggedInUser;
    private boolean useStdio = true;

    /**
     * Returns whether the client should automatically restart the server on crash.
     *
     * @return the auto-restart flag value (no longer has any effect)
     * @deprecated This option has no effect and will be removed in a future
     *             release.
     */
    @Deprecated
    public boolean isAutoRestart() {
        return autoRestart;
    }

    /**
     * Sets whether the client should automatically restart the CLI server if it
     * crashes unexpectedly.
     *
     * @param autoRestart
     *            ignored — this option no longer has any effect
     * @return this options instance for method chaining
     * @deprecated This option has no effect and will be removed in a future
     *             release.
     */
    @Deprecated
    public CopilotClientOptions setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
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
     * Gets the extra CLI arguments.
     * <p>
     * Returns a shallow copy of the internal array, or {@code null} if no arguments
     * have been set.
     *
     * @return a copy of the extra arguments, or {@code null}
     */
    public String[] getCliArgs() {
        return cliArgs != null ? Arrays.copyOf(cliArgs, cliArgs.length) : null;
    }

    /**
     * Sets extra arguments to pass to the CLI process.
     * <p>
     * These arguments are prepended before SDK-managed flags. A shallow copy of the
     * provided array is stored. If {@code null} or empty, the existing arguments
     * are cleared.
     *
     * @param cliArgs
     *            the extra arguments to pass, or {@code null}/empty to clear
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setCliArgs(String[] cliArgs) {
        if (cliArgs == null || cliArgs.length == 0) {
            if (this.cliArgs != null) {
                this.cliArgs = new String[0];
            }
        } else {
            this.cliArgs = Arrays.copyOf(cliArgs, cliArgs.length);
        }
        return this;
    }

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
     *            the path to the CLI executable
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setCliPath(String cliPath) {
        this.cliPath = Objects.requireNonNull(cliPath, "cliPath must not be null");
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
     *            the CLI server URL to connect to (must not be {@code null} or
     *            empty)
     * @return this options instance for method chaining
     * @throws IllegalArgumentException
     *             if {@code cliUrl} is {@code null} or empty
     */
    public CopilotClientOptions setCliUrl(String cliUrl) {
        this.cliUrl = Objects.requireNonNull(cliUrl, "cliUrl must not be null");
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
     *            the working directory path (must not be {@code null} or empty)
     * @return this options instance for method chaining
     * @throws IllegalArgumentException
     *             if {@code cwd} is {@code null} or empty
     */
    public CopilotClientOptions setCwd(String cwd) {
        this.cwd = Objects.requireNonNull(cwd, "cwd must not be null");
        return this;
    }

    /**
     * Gets the environment variables for the CLI process.
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
     * Sets environment variables to pass to the CLI process.
     * <p>
     * When set, these environment variables replace the inherited environment. A
     * shallow copy of the provided map is stored. If {@code null} or empty, the
     * existing environment is cleared.
     *
     * @param environment
     *            the environment variables map, or {@code null}/empty to clear
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setEnvironment(Map<String, String> environment) {
        if (environment == null || environment.isEmpty()) {
            if (this.environment != null) {
                this.environment.clear();
            }
        } else {
            this.environment = new HashMap<>(environment);
        }
        return this;
    }

    /**
     * Gets the executor used for internal asynchronous operations.
     *
     * @return the executor, or {@code null} to use the default
     *         {@code ForkJoinPool.commonPool()}
     */
    public Executor getExecutor() {
        return executor;
    }

    /**
     * Sets the executor used for internal asynchronous operations.
     * <p>
     * When provided, the SDK uses this executor for all internal
     * {@code CompletableFuture} combinators instead of the default
     * {@code ForkJoinPool.commonPool()}. This allows callers to isolate SDK work
     * onto a dedicated thread pool or integrate with container-managed threading.
     * <p>
     * Passing {@code null} reverts to the default {@code ForkJoinPool.commonPool()}
     * behavior.
     *
     * @param executor
     *            the executor to use, or {@code null} for the default
     * @return this options instance for fluent chaining
     */
    public CopilotClientOptions setExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    /**
     * Gets the GitHub token for authentication.
     *
     * @return the GitHub token, or {@code null} to use other authentication methods
     */
    public String getGitHubToken() {
        return gitHubToken;
    }

    /**
     * Sets the GitHub token to use for authentication.
     * <p>
     * When provided, the token is passed to the CLI server via environment
     * variable. This takes priority over other authentication methods.
     *
     * @param gitHubToken
     *            the GitHub token (must not be {@code null} or empty)
     * @return this options instance for method chaining
     * @throws IllegalArgumentException
     *             if {@code gitHubToken} is {@code null} or empty
     */
    public CopilotClientOptions setGitHubToken(String gitHubToken) {
        this.gitHubToken = Objects.requireNonNull(gitHubToken, "gitHubToken must not be null");
        return this;
    }

    /**
     * Gets the GitHub token for authentication.
     *
     * @return the GitHub token, or {@code null} to use other authentication methods
     * @deprecated Use {@link #getGitHubToken()} instead.
     */
    @Deprecated
    public String getGithubToken() {
        return gitHubToken;
    }

    /**
     * Sets the GitHub token to use for authentication.
     *
     * @param githubToken
     *            the GitHub token
     * @return this options instance for method chaining
     * @deprecated Use {@link #setGitHubToken(String)} instead.
     */
    @Deprecated
    public CopilotClientOptions setGithubToken(String githubToken) {
        this.gitHubToken = Objects.requireNonNull(githubToken, "githubToken must not be null");
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
     *            the log level (must not be {@code null} or empty)
     * @return this options instance for method chaining
     * @throws IllegalArgumentException
     *             if {@code logLevel} is {@code null} or empty
     */
    public CopilotClientOptions setLogLevel(String logLevel) {
        this.logLevel = Objects.requireNonNull(logLevel, "logLevel must not be null");
        return this;
    }

    /**
     * Gets the custom handler for listing available models.
     *
     * @return the handler, or {@code null} if not set
     */
    public Supplier<CompletableFuture<List<ModelInfo>>> getOnListModels() {
        return onListModels;
    }

    /**
     * Sets a custom handler for listing available models.
     * <p>
     * When provided, {@code listModels()} calls this handler instead of querying
     * the CLI server. Useful in BYOK (Bring Your Own Key) mode to return models
     * available from your custom provider.
     *
     * @param onListModels
     *            the handler that returns the list of available models (must not be
     *            {@code null})
     * @return this options instance for method chaining
     * @throws IllegalArgumentException
     *             if {@code onListModels} is {@code null}
     */
    public CopilotClientOptions setOnListModels(Supplier<CompletableFuture<List<ModelInfo>>> onListModels) {
        this.onListModels = Objects.requireNonNull(onListModels, "onListModels must not be null");
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
     * Gets the OpenTelemetry configuration for the CLI server.
     *
     * @return the telemetry config, or {@code null}
     * @since 1.2.0
     */
    public TelemetryConfig getTelemetry() {
        return telemetry;
    }

    /**
     * Sets the OpenTelemetry configuration for the CLI server.
     * <p>
     * When set, the CLI server is started with OpenTelemetry instrumentation
     * enabled using the provided settings.
     *
     * @param telemetry
     *            the telemetry configuration
     * @return this options instance for method chaining
     * @since 1.2.0
     */
    public CopilotClientOptions setTelemetry(TelemetryConfig telemetry) {
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry must not be null");
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
     * auth. When false, only explicit tokens (gitHubToken or environment variables)
     * are used. Default: true (but defaults to false when gitHubToken is provided).
     * <p>
     * Passing {@code null} is equivalent to passing {@link Boolean#FALSE}.
     *
     * @param useLoggedInUser
     *            {@code true} to use logged-in user auth, {@code false} or
     *            {@code null} otherwise
     * @return this options instance for method chaining
     */
    public CopilotClientOptions setUseLoggedInUser(Boolean useLoggedInUser) {
        this.useLoggedInUser = useLoggedInUser != null ? useLoggedInUser : Boolean.FALSE;
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
     * Creates a shallow clone of this {@code CopilotClientOptions} instance.
     * <p>
     * Array properties (like {@code cliArgs}) are copied into new arrays so that
     * modifications to the clone do not affect the original. The
     * {@code environment} map is also copied to a new map instance. Other
     * reference-type properties are shared between the original and clone.
     *
     * @return a clone of this options instance
     */
    @Override
    public CopilotClientOptions clone() {
        CopilotClientOptions copy = new CopilotClientOptions();
        copy.autoRestart = this.autoRestart;
        copy.autoStart = this.autoStart;
        copy.cliArgs = this.cliArgs != null ? this.cliArgs.clone() : null;
        copy.cliPath = this.cliPath;
        copy.cliUrl = this.cliUrl;
        copy.cwd = this.cwd;
        copy.environment = this.environment != null ? new java.util.HashMap<>(this.environment) : null;
        copy.executor = this.executor;
        copy.gitHubToken = this.gitHubToken;
        copy.logLevel = this.logLevel;
        copy.onListModels = this.onListModels;
        copy.port = this.port;
        copy.telemetry = this.telemetry;
        copy.useLoggedInUser = this.useLoggedInUser;
        copy.useStdio = this.useStdio;
        return copy;
    }
}
