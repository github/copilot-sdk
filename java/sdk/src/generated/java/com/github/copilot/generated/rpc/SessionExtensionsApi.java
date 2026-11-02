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
 * API methods for the {@code extensions} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionExtensionsApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionExtensionsApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Lists extensions discovered for the session and their current status.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionExtensionsListResult> list() {
        return caller.invoke("session.extensions.list", java.util.Map.of("sessionId", this.sessionId), SessionExtensionsListResult.class);
    }

    /**
     * Enables an extension for the session and persists the preference when the session has a settings store. Hosts synchronizing effective membership should use extensions.reconcile instead.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> enable(SessionExtensionsEnableParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.extensions.enable", _p, Void.class);
    }

    /**
     * Disables an extension for the session and persists the preference when the session has a settings store. Hosts synchronizing effective membership should use extensions.reconcile instead.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> disable(SessionExtensionsDisableParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.extensions.disable", _p, Void.class);
    }

    /**
     * Reloads extension definitions and processes for the session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> reload() {
        return caller.invoke("session.extensions.reload", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Host-only reconciliation of authoritative session-effective extension membership and enablement. Refreshes runtime-owned discovery and preferences without persisting settings, installing plugins, or restarting unchanged activations. Returns ExtensionList only after required starts and process/contribution cleanup settle. Takes no caller inventory or overrides. Missing controllers, unready/incomplete discovery, unavailable workspaces, superseded inputs, and lifecycle failures are errors, not empty membership. Independently proven revocations may be applied before an error; retry converges without restarting healthy activations. Error data contains lifecycleChangesApplied and code: extension_reconciliation_host_required, extension_reconciliation_unavailable, extension_reconciliation_not_ready, extension_reconciliation_discovery_failed, extension_reconciliation_workspace_unavailable, extension_reconciliation_superseded, or extension_reconciliation_lifecycle_failed. Mark host reconciliation state applied only on success. On older runtimes, method-not-found must not fall back to global discovery and persistent extension disables.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionExtensionsReconcileResult> reconcile() {
        return caller.invoke("session.extensions.reconcile", java.util.Map.of("sessionId", this.sessionId), SessionExtensionsReconcileResult.class);
    }

    /**
     * Push attachments into the next user-message turn from an extension. The host should surface them as composer pills and forward them via the next session.send call. Callable only by extension-owned connections.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> sendAttachmentsToMessage(SessionExtensionsSendAttachmentsToMessageParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.extensions.sendAttachmentsToMessage", _p, Void.class);
    }

}
