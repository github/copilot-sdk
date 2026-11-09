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
 * API methods for the {@code managedSettings} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionManagedSettingsApi {

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionManagedSettingsApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Reads the current retained managed-settings event payload without waiting for application or publication. Returns null when no snapshot exists. It does not fetch policy or alter startup ordering.
     *
     * @return a future that completes with the {@code ManagedSettingsResolvedData} value,
     *     or {@code null} when the result is absent. Callers must handle the
     *     {@code null} completion value.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<ManagedSettingsResolvedData> getCurrentSnapshot() {
        return caller.invoke("session.managedSettings.getCurrentSnapshot", java.util.Map.of("sessionId", this.sessionId), ManagedSettingsResolvedData.class);
    }

    /**
     * Waits for the session's existing policy ingest and tracked applications. Preserves enforcement failures, detached event delivery, and resource replacement checks without starting a new fetch.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> awaitApplied() {
        return caller.invoke("session.managedSettings.awaitApplied", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Waits only for the existing policy commit, not enforcement or MCP startup. This barrier permits extension admission without a policy enforcement cycle. Preserves errors and generation checks without starting a new fetch.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> awaitPublished() {
        return caller.invoke("session.managedSettings.awaitPublished", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Waits for the live session's in-flight managed-settings application, then returns the retained effective snapshot used by runtime enforcement and by `session.managed_settings_resolved`. It does not perform another account, device, or server resolution, and rejects when resolution has not produced a snapshot.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionManagedSettingsGetResult> get() {
        return caller.invoke("session.managedSettings.get", java.util.Map.of("sessionId", this.sessionId), SessionManagedSettingsGetResult.class);
    }

    /**
     * Runs a fresh Policy Helper evaluation for the live session's current account, registration, working directory, repository and client context, bypassing only a completed cached helper result for that exact context while coalescing an equivalent execution already in flight. The previous failure and safeguards remain active until fresh success is applied. Returns the same retained snapshot as session.managedSettings.get after the recheck settles.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionManagedSettingsGetResult> recheckPolicyHelper() {
        return caller.invoke("session.managedSettings.recheckPolicyHelper", java.util.Map.of("sessionId", this.sessionId), SessionManagedSettingsGetResult.class);
    }

}
