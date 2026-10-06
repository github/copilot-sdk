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
 * API methods for the {@code gitHubOwners} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerGitHubOwnersApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerGitHubOwnersApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Registers a cancellable owner listing and returns its request id. Separate from `gitHubOwners.list` so the id exists before the listing starts: a caller that abandons the listing the moment it begins would otherwise have nothing to name in `gitHubOwners.cancel`. The id serves one listing only. Long-abandoned unused ids can be released by later allocations.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubOwnersRequestIdResult> nextRequestId() {
        return caller.invoke("gitHubOwners.nextRequestId", java.util.Map.of(), GitHubOwnersRequestIdResult.class);
    }

    /**
     * Lists the logins the authenticated user may act as — their own account first, then the organizations they belong to — by asking the GitHub API under the supplied credential. No credential travels in the request: `authInfo` selects one the runtime already holds, and the runtime resolves the token and the GitHub host from it. A failure the caller should render arrives as `message`; one it should raise arrives as `throwError`.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubOwnersListResult> list(GitHubOwnersListParams params) {
        return caller.invoke("gitHubOwners.list", params, GitHubOwnersListResult.class);
    }

    /**
     * Abandons an owner listing started with the given request id. Answers `canceled: true` while a listing with that id is running. Answers `canceled: false` when the id was never registered, was registered but not used, was released after being abandoned, or its listing has ended. Canceling an unused id releases it, and a later `list` with that id is refused. The cancel acts only on owner listings and never reaches another request of the host.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubOwnersCancelResult> cancel(GitHubOwnersCancelParams params) {
        return caller.invoke("gitHubOwners.cancel", params, GitHubOwnersCancelResult.class);
    }

}
