/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code permissions.paths} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionPermissionsPathsApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionPermissionsPathsApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * No parameters; returns the session's allow-listed directories.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsPathsListResult> list() {
        return caller.invoke("session.permissions.paths.list", java.util.Map.of("sessionId", this.sessionId), SessionPermissionsPathsListResult.class);
    }

    /**
     * Directory path to add to the session's allowed directories.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsPathsAddResult> add(SessionPermissionsPathsAddParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.paths.add", _p, SessionPermissionsPathsAddResult.class);
    }

    /**
     * Directory path to set as the session's new primary working directory.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsPathsUpdatePrimaryResult> updatePrimary(SessionPermissionsPathsUpdatePrimaryParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.paths.updatePrimary", _p, SessionPermissionsPathsUpdatePrimaryResult.class);
    }

    /**
     * Path to evaluate against the session's allowed directories.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsPathsIsPathWithinAllowedDirectoriesResult> isPathWithinAllowedDirectories(SessionPermissionsPathsIsPathWithinAllowedDirectoriesParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.paths.isPathWithinAllowedDirectories", _p, SessionPermissionsPathsIsPathWithinAllowedDirectoriesResult.class);
    }

    /**
     * Path to evaluate against the session's workspace (primary) directory.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    public CompletableFuture<SessionPermissionsPathsIsPathWithinWorkspaceResult> isPathWithinWorkspace(SessionPermissionsPathsIsPathWithinWorkspaceParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.permissions.paths.isPathWithinWorkspace", _p, SessionPermissionsPathsIsPathWithinWorkspaceResult.class);
    }

}
