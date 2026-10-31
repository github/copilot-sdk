/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.concurrent.CompletableFuture;

import com.github.copilot.CopilotExperimental;
import com.github.copilot.generated.rpc.EntraTokenAcquireRequest;
import com.github.copilot.generated.rpc.EntraTokenAcquireResult;

/** Host-owned Entra identity or resource-token acquisition. */
@CopilotExperimental
@FunctionalInterface
public interface EntraTokenProvider {
    /**
     * Selects/revalidates an identity or acquires its resource token. Renewal must
     * be silent and account-pinned; unavailability and cancellation fail closed.
     *
     * @param request
     *            the requested acquisition and exact resource/account targeting
     * @param context
     *            cancelled when the request or original connection ends
     * @return the matching identity, token, unavailable, or cancelled result
     */
    CompletableFuture<EntraTokenAcquireResult> getToken(EntraTokenAcquireRequest request,
            EntraTokenProviderContext context);
}
