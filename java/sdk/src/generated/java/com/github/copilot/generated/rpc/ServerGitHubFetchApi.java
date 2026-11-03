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
 * API methods for the {@code gitHubFetch} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerGitHubFetchApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerGitHubFetchApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Registers one authenticated GitHub API request before it starts, so cancellation can name the operation. Each request claims its id once. Refused for extensions.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubFetchRequestId> allocateRequestId() {
        return caller.invoke("gitHubFetch.allocateRequestId", java.util.Map.of(), GitHubFetchRequestId.class);
    }

    /**
     * Calls the GitHub API with session credentials, or process credentials when sessionId is absent. A bound host can select its existing auth handle with authOwnerId. The URL must match that identity's API origin or the runtime's explicit debug API origin. Retries and eligible redirects resolve the current credential again. Explicit caller Authorization remains unchanged; cross-origin redirects discard credentials without restoring them on later hops. Release asset bodies require gitHubRelease.downloadAsset. JSON asset metadata and other response bodies remain supported. The runtime supplies a default User-Agent and records request diagnostics. Extensions cannot use this method.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubFetchResult> request(GitHubFetchRequestParams params) {
        return caller.invoke("gitHubFetch.request", params, GitHubFetchResult.class);
    }

    /**
     * Executes a named GitHub or Copilot job operation. The runtime derives its URL, headers, retry policy, and credential. sessionId selects session auth; omission selects process auth. A bound host connection can select its existing auth handle with authOwnerId. Retries and eligible redirects resolve the current credential again. Cross-origin redirects discard credentials without restoring them on later hops. Non-success HTTP responses include the formatted error for the host. Extensions cannot use this method.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubFetchResult> executeOperation(GitHubFetchExecuteOperationParams params) {
        return caller.invoke("gitHubFetch.executeOperation", params, GitHubFetchResult.class);
    }

    /**
     * Cancels a registered GitHub request. False means the id is no longer registered. Uses the same runtime request registry as request and executeOperation. Refused for extensions.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<GitHubFetchCancelResult> cancel(GitHubFetchCancelParams params) {
        return caller.invoke("gitHubFetch.cancel", params, GitHubFetchCancelResult.class);
    }

}
