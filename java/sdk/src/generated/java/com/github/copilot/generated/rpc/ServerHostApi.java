/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code host} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerHostApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerHostApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Publishes a resident session attached to the listener's owning connection.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<HostPublishSessionResult> publishSession(HostPublishSessionParams params) {
        return caller.invoke("host.publishSession", params, HostPublishSessionResult.class);
    }

    /**
     * One application-owned session handoff, requested by the supervised hosting participant.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<HostCreateSessionResult> createSession(HostCreateSessionParams params) {
        return caller.invoke("host.createSession", params, HostCreateSessionResult.class);
    }

    /**
     * Ends one participation, not the application's session lifetime.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> releaseSession(HostReleaseSessionParams params) {
        return caller.invoke("host.releaseSession", params, Void.class);
    }

    /**
     * Starts a supervised AHP host with at least one explicitly selected transport.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<HostStartResult> start(HostStartParams params) {
        return caller.invoke("host.start", params, HostStartResult.class);
    }

    /**
     * Stops a connection-owned listener and joins its teardown.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> dispose(HostDisposeParams params) {
        return caller.invoke("host.dispose", params, Void.class);
    }

    /**
     * Empty acknowledgement for a completed host lifecycle operation.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<HostGetEnvironmentCredentialsResult> getEnvironmentCredentials() {
        return caller.invoke("host.getEnvironmentCredentials", java.util.Map.of(), HostGetEnvironmentCredentialsResult.class);
    }

    /**
     * Empty acknowledgement for a completed host lifecycle operation.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<HostGetConfigurationResult> getConfiguration() {
        return caller.invoke("host.getConfiguration", java.util.Map.of(), HostGetConfigurationResult.class);
    }

    /**
     * Readiness reported by the supervised hosting participant on its own SDK connection.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> ready(HostReadyParams params) {
        return caller.invoke("host.ready", params, Void.class);
    }

}
