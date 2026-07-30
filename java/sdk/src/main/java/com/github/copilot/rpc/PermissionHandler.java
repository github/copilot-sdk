/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

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
 * 	if (Boolean.TRUE.equals(request.getManagedApprovalRequired())) {
 * 		// Obtain an explicit human decision before approving this request.
 * 		return requestHumanApproval(request);
 * 	}
 *
 * 	// Check the permission kind
 * 	if ("dangerous-action".equals(request.getKind())) {
 * 		// Deny dangerous actions
 * 		return CompletableFuture
 * 				.completedFuture(new PermissionRequestResult().setKind(PermissionRequestResultKind.REJECTED));
 * 	}
 *
 * 	// Approve other requests
 * 	return CompletableFuture
 * 			.completedFuture(new PermissionRequestResult().setKind(PermissionRequestResultKind.APPROVED));
 * };
 * }</pre>
 * <p>
 * Event-based permission dispatch can use
 * {@link PermissionRequestResult#noResult()} to let another connected client
 * answer a pending request. Legacy protocol-v2 callbacks require a decision and
 * cannot abstain.
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
     * A pre-built handler that approves permission requests when managed settings
     * are disabled.
     *
     * @since 1.0.11
     */
    PermissionHandler APPROVE_ALL = (request, invocation) -> {
        if (invocation.isManagedSettingsEnabled()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("APPROVE_ALL cannot be used when managed settings are enabled"));
        }
        if (Boolean.TRUE.equals(request.getManagedApprovalRequired())) {
            return CompletableFuture.completedFuture(PermissionRequestResult.noResult());
        }
        return CompletableFuture.completedFuture(PermissionRequestResult.approveOnce());
    };

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
