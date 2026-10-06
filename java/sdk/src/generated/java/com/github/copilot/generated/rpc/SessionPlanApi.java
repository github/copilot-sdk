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
 * API methods for the {@code plan} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionPlanApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionPlanApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Reads the session plan file from the workspace.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPlanReadResult> read() {
        return caller.invoke("session.plan.read", java.util.Map.of("sessionId", this.sessionId), SessionPlanReadResult.class);
    }

    /**
     * Writes new content to the session plan file.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> update(SessionPlanUpdateParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.plan.update", _p, Void.class);
    }

    /**
     * Deletes the session plan file from the workspace.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> delete() {
        return caller.invoke("session.plan.delete", java.util.Map.of("sessionId", this.sessionId), Void.class);
    }

    /**
     * Reads todo rows from the session SQL database for plan rendering.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPlanReadSqlTodosResult> readSqlTodos() {
        return caller.invoke("session.plan.readSqlTodos", java.util.Map.of("sessionId", this.sessionId), SessionPlanReadSqlTodosResult.class);
    }

    /**
     * Reads todo rows AND dependency edges from the session SQL database for structured progress UI. Same defensive behavior as readSqlTodos — returns empty arrays when the database, tables, or columns aren't available. Clients should call this on session start and after every `session.todos_changed` event to refresh structured-UI rendering.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionPlanReadSqlTodosWithDependenciesResult> readSqlTodosWithDependencies() {
        return caller.invoke("session.plan.readSqlTodosWithDependencies", java.util.Map.of("sessionId", this.sessionId), SessionPlanReadSqlTodosWithDependenciesResult.class);
    }

}
