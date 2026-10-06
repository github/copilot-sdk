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
 * API methods for the {@code tools} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionToolsApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionToolsApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Executes one tool from the session's currently offered tool set through the native invocation pipeline.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> execute(SessionToolsExecuteParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tools.execute", _p, Void.class);
    }

    /**
     * Returns the Rust-owned built-in tool descriptors used to construct the session's offered tool set.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionToolsGetBuiltinDescriptorsResult> getBuiltinDescriptors(SessionToolsGetBuiltinDescriptorsParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tools.getBuiltinDescriptors", _p, SessionToolsGetBuiltinDescriptorsResult.class);
    }

    /**
     * Projects a completed task_complete tool call into its label-safe session event payload.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionToolsTaskCompleteEventDataResult> taskCompleteEventData(SessionToolsTaskCompleteEventDataParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tools.taskCompleteEventData", _p, SessionToolsTaskCompleteEventDataResult.class);
    }

    /**
     * Provides the result for a pending external tool call.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionToolsHandlePendingToolCallResult> handlePendingToolCall(SessionToolsHandlePendingToolCallParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tools.handlePendingToolCall", _p, SessionToolsHandlePendingToolCallResult.class);
    }

    /**
     * Resolves, builds, and validates the runtime tool list for the session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> initializeAndValidate() {
        return caller.invoke("session.tools.initializeAndValidate", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Returns lightweight metadata for the session's currently initialized tools.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionToolsGetCurrentMetadataResult> getCurrentMetadata() {
        return caller.invoke("session.tools.getCurrentMetadata", java.util.Map.of("sessionId", this.sessionId), SessionToolsGetCurrentMetadataResult.class);
    }

    /**
     * Atomically replaces the complete externally implemented tool list supplied by the calling connection. Built-in, MCP/plugin, extension-discovered, subagent, and tools supplied by other connections remain unchanged.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> set(SessionToolsSetParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tools.set", _p, Void.class);
    }

    /**
     * Sets the current session's live subagent settings override, which takes precedence over persisted user settings until cleared. Persisted user settings remain the source of truth for future sessions.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> updateSubagentSettings(SessionToolsUpdateSubagentSettingsParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.tools.updateSubagentSettings", _p, Void.class);
    }

}
