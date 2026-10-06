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
 * API methods for the {@code agent} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionAgentApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionAgentApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Lists agents available to the session. Defaults to custom agents only; pass includeBuiltInAgents to include the effective built-in agents.
     * <p>
     * Invokes the method with no params, applying the runtime defaults.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionAgentListResult> list() {
        return list(null);
    }

    /**
     * Lists agents available to the session. Defaults to custom agents only; pass includeBuiltInAgents to include the effective built-in agents.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionAgentListResult> list(SessionAgentListParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = params == null ? MAPPER.createObjectNode() : MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.agent.list", _p, SessionAgentListResult.class);
    }

    /**
     * Sets an in-memory authored prompt override for an available agent. For built-in agents, this replaces only the static base prompt while preserving runtime-owned dynamic prompt composition and behavior. The special `general-purpose` agent is not overrideable. Overrides are not persisted; resumed and forked sessions start without them, so the host must re-apply them.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> setPrompt(SessionAgentSetPromptParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.agent.setPrompt", _p, Void.class);
    }

    /**
     * Gets the currently selected custom agent for the session.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionAgentGetCurrentResult> getCurrent() {
        return caller.invoke("session.agent.getCurrent", java.util.Map.of("sessionId", this.sessionId), SessionAgentGetCurrentResult.class);
    }

    /**
     * Selects a custom agent for subsequent turns in the session.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionAgentSelectResult> select(SessionAgentSelectParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.agent.select", _p, SessionAgentSelectResult.class);
    }

    /**
     * Clears the selected custom agent and returns the session to the default agent.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> deselect() {
        return caller.invoke("session.agent.deselect", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Reloads custom agent definitions and returns the refreshed list.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionAgentReloadResult> reload() {
        return caller.invoke("session.agent.reload", java.util.Map.of("sessionId", this.sessionId), SessionAgentReloadResult.class);
    }

}
