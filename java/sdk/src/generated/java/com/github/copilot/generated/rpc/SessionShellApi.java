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
 * API methods for the {@code shell} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionShellApi {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;

    private final RpcCaller caller;
    private final String sessionId;

    /** @param caller the RPC transport function */
    SessionShellApi(RpcCaller caller, String sessionId) {
        this.caller = caller;
        this.sessionId = sessionId;
    }

    /**
     * Starts a shell command, returning an RPC error if it cannot be spawned. The command runs as the leader of its own process group (POSIX) or in a dedicated job object (Windows), so a forced termination — via "shell.kill", the request timeout, or session disposal — signals that whole group/job rather than only the direct child. Two gaps are worth planning for: a command that exits on its own does not trigger that teardown, and on POSIX a descendant that moves itself into a new session or process group (for example via "setsid") leaves the signalled group, so either can leave a background process running.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionShellExecResult> exec(SessionShellExecParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.shell.exec", _p, SessionShellExecResult.class);
    }

    /**
     * Sends a signal to a shell process previously started via "shell.exec". The signal targets the command's whole process group (POSIX) or job object (Windows), so descendants still in that group are signalled too, not just the direct child. On POSIX a descendant that moved itself into a new session or process group (for example via "setsid") is no longer in the signalled group and survives.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionShellKillResult> kill(SessionShellKillParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.shell.kill", _p, SessionShellKillResult.class);
    }

    /**
     * Executes a user-requested shell command through the session runtime.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionShellExecuteUserRequestedResult> executeUserRequested(SessionShellExecuteUserRequestedParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.shell.executeUserRequested", _p, SessionShellExecuteUserRequestedResult.class);
    }

    /**
     * Cancels a user-requested shell command by request ID.
     * <p>
     * Note: the {@code sessionId} field in the params record is overridden
     * by the session-scoped wrapper; any value provided is ignored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<SessionShellCancelUserRequestedResult> cancelUserRequested(SessionShellCancelUserRequestedParams params) {
        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);
        _p.put("sessionId", this.sessionId);
        return caller.invoke("session.shell.cancelUserRequested", _p, SessionShellCancelUserRequestedResult.class);
    }

}
