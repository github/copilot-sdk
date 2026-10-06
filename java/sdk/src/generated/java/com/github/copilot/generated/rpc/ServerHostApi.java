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
     * Publishes an attached resident session for this listener's lifetime without copying it.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<HostPublishSessionResult> publishSession(HostPublishSessionParams params) {
        return caller.invoke("host.publishSession", params, HostPublishSessionResult.class);
    }

    /**
     * Requests app-owned materialization over the owning SDK participant.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<HostSessionCreateResult> createSession(HostCreateSessionParams params) {
        return caller.invoke("host.createSession", params, HostSessionCreateResult.class);
    }

    /**
     * Releases app ownership retention after AHP detaches.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> releaseSession(HostReleaseSessionParams params) {
        return caller.invoke("host.releaseSession", params, Void.class);
    }

    /**
     * Starts a connection-owned AHP host with explicit localServer and/or githubEnvironment transports as a supervised SDK participant.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<HostStartResult> start(HostStartParams params) {
        return caller.invoke("host.start", params, HostStartResult.class);
    }

    /**
     * Stops a listener owned by this SDK connection and joins its cleanup without deleting sessions.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> dispose(HostDisposeParams params) {
        return caller.invoke("host.dispose", params, Void.class);
    }

    /**
     * Resolves current authenticated credentials and remote-control policy only for the runtime-owned Mission Control hosting participant.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<HostEnvironmentCredentials> getEnvironmentCredentials() {
        return caller.invoke("host.getEnvironmentCredentials", java.util.Map.of(), HostEnvironmentCredentials.class);
    }

    /**
     * Returns listener settings only to the supervised hosting participant over its SDK connection.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<HostConfiguration> getConfiguration() {
        return caller.invoke("host.getConfiguration", java.util.Map.of(), HostConfiguration.class);
    }

    /**
     * Reports a supervised hosting participant's bound AHP endpoint after its SDK handshake.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> ready(HostReadyParams params) {
        return caller.invoke("host.ready", params, Void.class);
    }

}
