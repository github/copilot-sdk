/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.ExtensionLaunchProviderResolveRequest;
import com.github.copilot.generated.rpc.ExtensionLaunchProviderResolveResult;
import com.github.copilot.rpc.ExtensionLaunchProvider;

/**
 * Bridges {@code extensionLaunchProvider.resolve} reverse RPC calls to the
 * configured extension launch provider.
 */
final class ExtensionLaunchProviderAdapter {

    private static final Logger LOG = Logger.getLogger(ExtensionLaunchProviderAdapter.class.getName());
    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    private final ExtensionLaunchProvider provider;

    ExtensionLaunchProviderAdapter(ExtensionLaunchProvider provider) {
        this.provider = provider;
    }

    void registerHandlers(JsonRpcClient rpc) {
        rpc.registerMethodHandler("extensionLaunchProvider.resolve",
                (rpcId, params) -> handleResolve(rpc, rpcId, params));
    }

    private void handleResolve(JsonRpcClient rpc, String rpcId, JsonNode params) {
        if (rpcId == null) {
            return;
        }

        try {
            ExtensionLaunchProviderResolveRequest request = MAPPER.treeToValue(params,
                    ExtensionLaunchProviderResolveRequest.class);
            CompletableFuture<ExtensionLaunchProviderResolveResult> resolution = provider.resolve(request);
            if (resolution == null) {
                sendError(rpc, rpcId, "Extension launch provider returned a null future");
                return;
            }
            resolution.whenComplete((result, error) -> {
                if (error != null) {
                    Throwable cause = error instanceof CompletionException && error.getCause() != null
                            ? error.getCause()
                            : error;
                    sendError(rpc, rpcId, cause.getMessage() != null ? cause.getMessage() : cause.toString());
                    return;
                }
                try {
                    rpc.sendResponse(parseRpcId(rpcId), result);
                } catch (IOException e) {
                    LOG.log(Level.FINE, "Failed to send extension launch provider response", e);
                }
            });
        } catch (Exception e) {
            sendError(rpc, rpcId, e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private static void sendError(JsonRpcClient rpc, String rpcId, String message) {
        try {
            rpc.sendErrorResponse(parseRpcId(rpcId), -32603, message);
        } catch (IOException e) {
            LOG.log(Level.FINE, "Failed to send extension launch provider error", e);
        }
    }

    private static Object parseRpcId(String rpcId) {
        try {
            return Long.valueOf(rpcId);
        } catch (NumberFormatException ignored) {
            return rpcId;
        }
    }
}
