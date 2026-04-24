/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code workspaces} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionWorkspacesApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionWorkspacesApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Invokes {@code session.workspaces.getWorkspace}.
     * @since 1.0.0
     */
    public CompletableFuture<SessionWorkspacesGetWorkspaceResult> getWorkspace() {
        return caller.invoke("session.workspaces.getWorkspace", java.util.Map.of("sessionId", this.sessionId), SessionWorkspacesGetWorkspaceResult.class);
    }

    /**
     * Invokes {@code session.workspaces.listFiles}.
     * @since 1.0.0
     */
    public CompletableFuture<SessionWorkspacesListFilesResult> listFiles() {
        return caller.invoke("session.workspaces.listFiles", java.util.Map.of("sessionId", this.sessionId), SessionWorkspacesListFilesResult.class);
    }

    /**
     * Invokes {@code session.workspaces.readFile}.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     * @since 1.0.0
     */
    public CompletableFuture<SessionWorkspacesReadFileResult> readFile(SessionWorkspacesReadFileParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.readFile", _p, SessionWorkspacesReadFileResult.class);
    }

    /**
     * Invokes {@code session.workspaces.createFile}.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     * @since 1.0.0
     */
    public CompletableFuture<Void> createFile(SessionWorkspacesCreateFileParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.workspaces.createFile", _p, Void.class);
    }

}
