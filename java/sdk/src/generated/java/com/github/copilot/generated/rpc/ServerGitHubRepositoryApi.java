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

    /**
     * Resolves the owner and name of a GitHub repository from its numeric repository id, by asking the GitHub API under the supplied credential. Returns a null `repository` when the id names nothing the credential can see, which the API reports as a 404, so a caller can tell 'no such visible repository' apart from a request that failed. The host queried is derived from `authInfo` rather than supplied separately, so the lookup always runs against the account's own GitHub host.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubRepositoryByIdResult> getById(GitHubRepositoryGetByIdParams params) {
        return caller.invoke("gitHubRepository.getById", params, GitHubRepositoryByIdResult.class);
    }

    /**
     * Finds the pull request a branch is open against, searching every repository in `baseRepositories` so a fork's branch finds the pull request it opened upstream. A candidate is discarded unless `headOwner` owns its head branch, because a branch name alone is ambiguous across forks. `status` separates 'not authenticated', 'no such pull request' and 'the search failed', so a caller can tell a conclusive absence apart from an unknown one. The host queried is derived from `authInfo` rather than supplied separately, so the lookup always runs against the account's own GitHub host.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubBranchPullRequestLookupResult> lookupBranchPullRequest(GitHubRepositoryLookupBranchPullRequestParams params) {
        return caller.invoke("gitHubRepository.lookupBranchPullRequest", params, GitHubBranchPullRequestLookupResult.class);
    }

}
