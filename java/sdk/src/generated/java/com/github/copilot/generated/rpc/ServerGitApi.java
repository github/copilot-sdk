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
 * API methods for the {@code git} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerGitApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerGitApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Reads the remote that the branch checked out in a working tree tracks, as `branch.<name>.remote` configures it. Reports `origin` rather than failing whenever there is no tracking configuration to read — on a detached HEAD, on a branch with no upstream, or when git itself fails — because a caller asking which remote to talk to needs an answer it can act on, not an error. Marked internal because it exists to carry a CLI call site off the napi boundary onto the SDK contract; it is migration plumbing, not a surface consumers are meant to depend on.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitCurrentBranchRemoteResult> currentBranchRemote(GitCurrentBranchRemoteParams params) {
        return caller.invoke("git.currentBranchRemote", params, GitCurrentBranchRemoteResult.class);
    }

    /**
     * Collects the repository context of a working directory in one call: working tree root, repository identifier and host, current branch, and the HEAD and base commits. Every repository field is omitted when the path is not inside a git working tree, and the requested path is echoed back as `cwd`. The answer is the same `SessionWorkingDirectoryContext` that `session.metadata.recordContextChange` accepts, so a caller polling for a context change can forward the result unchanged. Marked internal because it exists to carry a CLI call site off the napi boundary onto the SDK contract; it is migration plumbing, not a surface consumers are meant to depend on. It can become public once an SDK consumer needs to derive session context from a directory itself.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SessionWorkingDirectoryContext> workingDirectoryContext(GitWorkingDirectoryContextParams params) {
        return caller.invoke("git.workingDirectoryContext", params, SessionWorkingDirectoryContext.class);
    }

    /**
     * Lists the GitHub repositories a working tree's remotes point at, one entry per distinct repository, so a caller can resolve a base and head repository without parsing remote URLs itself. When several remotes name the same repository, only the first is listed, and the entry keeps that remote name. Remotes pointing at no GitHub host are left out, so an empty list means the tree reaches GitHub through no remote. Failing to read the remotes is reported as an error rather than as an empty list, because the two mean different things to a caller. Marked internal because it exists to carry a CLI call site off the napi boundary onto the SDK contract; it is migration plumbing, not a surface consumers are meant to depend on.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitReposFromRemotesResult> reposFromRemotes(GitReposFromRemotesParams params) {
        return caller.invoke("git.reposFromRemotes", params, GitReposFromRemotesResult.class);
    }

}
