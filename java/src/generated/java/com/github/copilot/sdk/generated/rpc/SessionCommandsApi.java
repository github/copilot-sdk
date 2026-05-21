/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code commands} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCommandsApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionCommandsApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Optional filters controlling which command sources to include in the listing.
     * @since 1.0.0
     */
    public CompletableFuture<SessionCommandsListResult> list() {
        return caller.invoke("session.commands.list", java.util.Map.of("sessionId", this.sessionId), SessionCommandsListResult.class);
    }

    /**
     * Slash command name and optional raw input string to invoke.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     * @since 1.0.0
     */
    public CompletableFuture<Void> invoke(SessionCommandsInvokeParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.commands.invoke", _p, Void.class);
    }

    /**
     * Pending command request ID and an optional error if the client handler failed.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     * @since 1.0.0
     */
    public CompletableFuture<SessionCommandsHandlePendingCommandResult> handlePendingCommand(SessionCommandsHandlePendingCommandParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.commands.handlePendingCommand", _p, SessionCommandsHandlePendingCommandResult.class);
    }

    /**
     * Queued command request ID and the result indicating whether the client handled it.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     * @since 1.0.0
     */
    public CompletableFuture<SessionCommandsRespondToQueuedCommandResult> respondToQueuedCommand(SessionCommandsRespondToQueuedCommandParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.commands.respondToQueuedCommand", _p, SessionCommandsRespondToQueuedCommandResult.class);
    }

}
