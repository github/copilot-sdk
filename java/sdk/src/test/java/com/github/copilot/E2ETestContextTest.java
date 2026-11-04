/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class E2ETestContextTest {

    @Test
    void defaultTelemetryEndpointUsesTheOwnedProxy() throws Exception {
        try (E2ETestContext ctx = E2ETestContext.create()) {
            var client = HttpClient.newHttpClient();
            var userRequest = HttpRequest.newBuilder(URI.create(ctx.getProxyUrl() + "/copilot_internal/user"))
                    .header("Authorization", "Bearer " + ctx.getEnvironment().get("GH_TOKEN")).build();
            var userResponse = client.send(userRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, userResponse.statusCode());
            var telemetryEndpoint = new ObjectMapper().readTree(userResponse.body()).path("endpoints").path("telemetry")
                    .asText();
            assertEquals(ctx.getProxyUrl(), telemetryEndpoint);
            var telemetryRequest = HttpRequest.newBuilder(URI.create(telemetryEndpoint + "/telemetry"))
                    .POST(HttpRequest.BodyPublishers.ofString("{}\n")).build();
            var telemetryResponse = client.send(telemetryRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(204, telemetryResponse.statusCode());
            assertEquals("", telemetryResponse.body());
        }
    }

    @Test
    void expectedUserPromptsParseFoldedBlockScalars() {
        String snapshot = """
                conversations:
                  - messages:
                      - role: user
                        content: First prompt
                          continued here.
                      - role: assistant
                        content: response
                      - role: user
                        content: >-
                          <system_notification>

                          Agent completed successfully.

                          </system_notification>
                """;

        assertEquals(
                List.of("First prompt continued here.",
                        "<system_notification>\nAgent completed successfully.\n</system_notification>"),
                E2ETestContext.parseExpectedUserPrompts(snapshot));
    }
}
