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
 * API methods for the {@code gitHubRepository} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerGitHubRepositoryApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerGitHubRepositoryApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Resolves the GitHub repository that owns a working-tree path by reading the selected git remote configured for it, preferring `origin`. Returns a null `repository` when the path is inside a git working tree but that selected remote does not resolve to a GitHub host. Fails when the path is not inside a git working tree at all, so a caller can tell 'not a repository' apart from 'a repository with no GitHub remote'.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubRepositoryAtPathResult> atPath(GitHubRepositoryAtPathParams params) {
        return caller.invoke("gitHubRepository.atPath", params, GitHubRepositoryAtPathResult.class);
    }

}
