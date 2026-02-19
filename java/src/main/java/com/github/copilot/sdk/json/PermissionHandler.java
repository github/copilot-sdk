/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.concurrent.CompletableFuture;

/**
 * Functional interface for handling permission requests from the AI assistant.
 * <p>
 * When the assistant needs permission to perform certain actions (such as
 * executing tools or accessing resources), this handler is invoked to approve
 * or deny the request.
 *
 * <h2>Example Implementation</h2>
 *
 * <pre>{@code
 * PermissionHandler handler = (request, invocation) -> {
 * 	// Check the permission kind
 * 	if ("dangerous-action".equals(request.getKind())) {
 * 		// Deny dangerous actions
 * 		return CompletableFuture.completedFuture(new PermissionRequestResult().setKind("user-denied"));
 * 	}
 *
 * 	// Approve other requests
 * 	return CompletableFuture.completedFuture(new PermissionRequestResult().setKind("user-approved"));
 * };
 * }</pre>
 *
 * <p>
 * A pre-built handler that approves all requests is available as
 * {@link #APPROVE_ALL}.
 *
 * @see SessionConfig#setOnPermissionRequest(PermissionHandler)
 * @see PermissionRequest
 * @see PermissionRequestResult
 * @since 1.0.0
 */
@FunctionalInterface
public interface PermissionHandler {

    /**
     * A pre-built handler that approves all permission requests.
     *
     * @since 1.0.11
     */
    PermissionHandler APPROVE_ALL = (request, invocation) -> CompletableFuture
            .completedFuture(new PermissionRequestResult().setKind("approved"));

    /**
     * Handles a permission request from the assistant.
     * <p>
     * The handler should evaluate the request and return a result indicating
     * whether the permission is granted or denied.
     *
     * @param request
     *            the permission request details
     * @param invocation
     *            the invocation context with session information
     * @return a future that completes with the permission decision
     */
    CompletableFuture<PermissionRequestResult> handle(PermissionRequest request, PermissionInvocation invocation);
}
