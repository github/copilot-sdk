/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.github.copilot.generated.rpc.GitHubTelemetryNotification;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

/**
 * Failsafe integration test that verifies the live CLI forwards GitHub
 * telemetry notifications during session creation.
 */
@AllowCopilotExperimental
class GitHubTelemetryForwardingIT {

    @Test
    void forwardsGitHubTelemetryForALiveSession() throws Exception {
        var notifications = new CopyOnWriteArrayList<GitHubTelemetryNotification>();
        var firstNotification = new CompletableFuture<GitHubTelemetryNotification>();

        try (E2ETestContext ctx = E2ETestContext.create()) {
            var options = new CopilotClientOptions().setOnGitHubTelemetry(notification -> {
                notifications.add(notification);
                firstNotification.complete(notification);
                return CompletableFuture.completedFuture(null);
            });

            try (CopilotClient client = ctx.createClient(options);
                    CopilotSession session = client
                            .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                            .get(30, TimeUnit.SECONDS)) {

                GitHubTelemetryNotification notification = firstNotification.get(30, TimeUnit.SECONDS);

                assertFalse(notifications.isEmpty(), "Expected at least one GitHub telemetry notification");
                assertNotNull(notification, "Expected a GitHub telemetry notification");
                assertNotNull(notification.sessionId(), "Telemetry notification sessionId must be present");
                assertTrue(!notification.sessionId().isBlank(), "Telemetry notification sessionId must be non-empty");
                assertNotNull(notification.restricted(), "Telemetry notification restricted flag must be present");
                assertNotNull(notification.event(), "Telemetry notification event must be present");
                assertNotNull(notification.event().kind(), "Telemetry event kind must be present");
                assertTrue(!notification.event().kind().isBlank(), "Telemetry event kind must be non-empty");
            }
        }
    }
}
