/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import com.github.copilot.generated.rpc.HostGitHubEnvironmentOptions;
import com.github.copilot.generated.rpc.HostLocalServerOptions;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Options for in-process AHP hosting. Select at least one transport explicitly;
 * no local listener is started by default. Factories must return the exact
 * session registered on the owning client with the requested identity and
 * settings. Release callbacks end a participation, not the application's
 * session lifetime.
 */
@CopilotExperimental
public final class AhpHostOptions {
    private String computeId;
    private HostLocalServerOptions localServer;
    private HostGitHubEnvironmentOptions githubEnvironment;
    private Function<AhpSessionCreateRequest, CompletableFuture<CopilotSession>> createSession;
    private Function<AhpSessionResumeRequest, CompletableFuture<CopilotSession>> resumeSession;
    private Function<CopilotSession, CompletableFuture<Void>> onSessionReleased;
    private Function<AhpHostExit, CompletableFuture<Void>> onExit;

    /**
     * Creates options without a transport. Set a local server, a GitHub
     * environment, or both before starting the host.
     */
    public AhpHostOptions() {
    }

    AhpHostOptions(AhpHostOptions options) {
        computeId = options.computeId;
        localServer = options.localServer;
        githubEnvironment = options.githubEnvironment;
        createSession = options.createSession;
        resumeSession = options.resumeSession;
        onSessionReleased = options.onSessionReleased;
        onExit = options.onExit;
    }

    /**
     * Gets the durable catalog's compute identity.
     *
     * @return the stable compute identity, or {@code null} for the runtime default
     */
    public String getComputeId() {
        return computeId;
    }

    /**
     * Selects the same durable catalog for local and Mission Control hosting. Must
     * agree with the GitHub environment's compute identity when both are supplied.
     *
     * @param computeId
     *            stable identity, or {@code null} for the runtime default
     * @return these options
     */
    public AhpHostOptions setComputeId(String computeId) {
        this.computeId = computeId;
        return this;
    }

    /**
     * Gets the local WebSocket transport settings.
     *
     * @return the settings, or {@code null} when local hosting is disabled
     */
    public HostLocalServerOptions getLocalServer() {
        return localServer;
    }

    /**
     * Enables or disables the local WebSocket transport.
     *
     * @param localServer
     *            local settings, or {@code null} to disable local hosting
     * @return these options
     */
    public AhpHostOptions setLocalServer(HostLocalServerOptions localServer) {
        this.localServer = localServer;
        return this;
    }

    /**
     * Gets the GitHub Mission Control transport settings.
     *
     * @return the settings, or {@code null} when GitHub hosting is disabled
     */
    public HostGitHubEnvironmentOptions getGithubEnvironment() {
        return githubEnvironment;
    }

    /**
     * Enables or disables GitHub Mission Control hosting.
     *
     * @param githubEnvironment
     *            settings with a required name and compute ID, or {@code null}
     * @return these options
     */
    public AhpHostOptions setGithubEnvironment(HostGitHubEnvironmentOptions githubEnvironment) {
        this.githubEnvironment = githubEnvironment;
        return this;
    }

    /**
     * Gets the creation callback.
     *
     * @return the application creation callback, or {@code null}
     */
    public Function<AhpSessionCreateRequest, CompletableFuture<CopilotSession>> getCreateSession() {
        return createSession;
    }

    /**
     * Sets the application creation callback.
     *
     * @param callback
     *            application creation callback
     * @return these options
     */
    public AhpHostOptions setCreateSession(
            Function<AhpSessionCreateRequest, CompletableFuture<CopilotSession>> callback) {
        createSession = callback;
        return this;
    }

    /**
     * Gets the resume callback.
     *
     * @return the application resume callback, or {@code null}
     */
    public Function<AhpSessionResumeRequest, CompletableFuture<CopilotSession>> getResumeSession() {
        return resumeSession;
    }

    /**
     * Sets the application resume callback.
     *
     * @param callback
     *            application resume callback
     * @return these options
     */
    public AhpHostOptions setResumeSession(
            Function<AhpSessionResumeRequest, CompletableFuture<CopilotSession>> callback) {
        resumeSession = callback;
        return this;
    }

    /**
     * Gets the release callback.
     *
     * @return the participation-release callback, or {@code null}
     */
    public Function<CopilotSession, CompletableFuture<Void>> getOnSessionReleased() {
        return onSessionReleased;
    }

    /**
     * Sets the participation-release callback.
     *
     * @param callback
     *            callback receiving the exact factory result once
     * @return these options
     */
    public AhpHostOptions setOnSessionReleased(Function<CopilotSession, CompletableFuture<Void>> callback) {
        onSessionReleased = callback;
        return this;
    }

    /**
     * Gets the exit callback.
     *
     * @return the listener exit callback, or {@code null}
     */
    public Function<AhpHostExit, CompletableFuture<Void>> getOnExit() {
        return onExit;
    }

    /**
     * Sets the listener exit callback.
     *
     * @param callback
     *            callback receiving the listener's final outcome once
     * @return these options
     */
    public AhpHostOptions setOnExit(Function<AhpHostExit, CompletableFuture<Void>> callback) {
        onExit = callback;
        return this;
    }
}
