/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code workspace} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionWorkspaceApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionWorkspaceApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Invokes {@code session.workspace.listFiles}.
     * @since 1.0.0
     */
    public CompletableFuture<SessionWorkspaceListFilesResult> listFiles() {
        return caller.invoke("session.workspace.listFiles", java.util.Map.of("sessionId", this.sessionId), SessionWorkspaceListFilesResult.class);
    }

    /**
     * Invokes {@code session.workspace.readFile}.
     * @since 1.0.0
     */
    public CompletableFuture<SessionWorkspaceReadFileResult> readFile(SessionWorkspaceReadFileParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspace.readFile", _p, SessionWorkspaceReadFileResult.class);
    }

    /**
     * Invokes {@code session.workspace.createFile}.
     * @since 1.0.0
     */
    public CompletableFuture<Void> createFile(SessionWorkspaceCreateFileParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspace.createFile", _p, Void.class);
    }

}
