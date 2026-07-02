/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.GitHubTelemetryNotification;

/**
 * Bridges the runtime's {@code gitHubTelemetry.event} client-global
 * notification to a consumer's async {@code onGitHubTelemetry} callback. The
 * notification carries per-session GitHub (hydro) telemetry the runtime
 * forwards to connections that opted into telemetry forwarding.
 */
final class GitHubTelemetryAdapter {

    private static final Logger LOG = Logger.getLogger(GitHubTelemetryAdapter.class.getName());
    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    private final Function<GitHubTelemetryNotification, CompletableFuture<Void>> callback;

    GitHubTelemetryAdapter(Function<GitHubTelemetryNotification, CompletableFuture<Void>> callback) {
        this.callback = callback;
    }

    void registerHandlers(JsonRpcClient rpc) {
        rpc.registerMethodHandler("gitHubTelemetry.event", (rpcId, params) -> handleEvent(params));
    }

    private void handleEvent(JsonNode params) {
        try {
            GitHubTelemetryNotification notification = MAPPER.treeToValue(params, GitHubTelemetryNotification.class);
            if (notification != null) {
                CompletableFuture<Void> result = callback.apply(notification);
                if (result != null) {
                    result.whenComplete((unused, error) -> {
                        if (error != null) {
                            LOG.log(Level.WARNING, "Error handling gitHubTelemetry.event notification", error);
                        }
                    });
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error handling gitHubTelemetry.event notification", e);
        }
    }
}
