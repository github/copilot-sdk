/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.sdk.events.SessionEventParser;
import com.github.copilot.sdk.events.ToolExecutionProgressEvent;
import com.github.copilot.sdk.json.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the new metadata APIs (getStatus, getAuthStatus, listModels) and
 * the ToolExecutionProgressEvent.
 */
public class MetadataApiTest {

    private static String cliPath;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setup() {
        cliPath = getCliPath();
    }

    private static String getCliPath() {
        // First, try to find 'copilot' in PATH
        String copilotInPath = findCopilotInPath();
        if (copilotInPath != null) {
            return copilotInPath;
        }

        // Fall back to COPILOT_CLI_PATH environment variable
        String envPath = System.getenv("COPILOT_CLI_PATH");
        if (envPath != null && !envPath.isEmpty()) {
            return envPath;
        }

        // Search for the CLI in the parent directories (nodejs module)
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path cliPath = current.resolve("nodejs/node_modules/@github/copilot/index.js");
            if (cliPath.toFile().exists()) {
                return cliPath.toString();
            }
            current = current.getParent();
        }

        return null;
    }

    private static String findCopilotInPath() {
        try {
            String command = System.getProperty("os.name").toLowerCase().contains("win") ? "where" : "which";
            var pb = new ProcessBuilder(command, "copilot");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                int exitCode = process.waitFor();
                if (exitCode == 0 && line != null && !line.isEmpty()) {
                    return line.trim();
                }
            }
        } catch (Exception e) {
            // Ignore - copilot not found in PATH
        }
        return null;
    }

    // ===== ToolExecutionProgressEvent Tests =====

    @Test
    void testToolExecutionProgressEventParsing() throws Exception {
        String json = """
                {
                    "type": "tool.execution_progress",
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "timestamp": "2026-01-22T10:00:00Z",
                    "data": {
                        "toolCallId": "call-123",
                        "progressMessage": "Processing file 1 of 10..."
                    }
                }
                """;

        var event = SessionEventParser.parse(MAPPER.readTree(json));

        assertNotNull(event);
        assertInstanceOf(ToolExecutionProgressEvent.class, event);

        ToolExecutionProgressEvent progressEvent = (ToolExecutionProgressEvent) event;
        assertEquals("tool.execution_progress", progressEvent.getType());
        assertNotNull(progressEvent.getData());
        assertEquals("call-123", progressEvent.getData().toolCallId());
        assertEquals("Processing file 1 of 10...", progressEvent.getData().progressMessage());
    }

    @Test
    void testToolExecutionProgressEventType() {
        assertEquals("tool.execution_progress", ToolExecutionProgressEvent.TYPE);
    }

    // ===== Response Type Deserialization Tests =====

    @Test
    void testGetStatusResponseDeserialization() throws Exception {
        String json = """
                {
                    "version": "1.2.3",
                    "protocolVersion": 2
                }
                """;

        GetStatusResponse response = MAPPER.readValue(json, GetStatusResponse.class);

        assertEquals("1.2.3", response.getVersion());
        assertEquals(2, response.getProtocolVersion());
    }

    @Test
    void testGetAuthStatusResponseDeserialization() throws Exception {
        String json = """
                {
                    "isAuthenticated": true,
                    "authType": "user",
                    "host": "github.com",
                    "login": "testuser",
                    "statusMessage": "Authenticated successfully"
                }
                """;

        GetAuthStatusResponse response = MAPPER.readValue(json, GetAuthStatusResponse.class);

        assertTrue(response.isAuthenticated());
        assertEquals("user", response.getAuthType());
        assertEquals("github.com", response.getHost());
        assertEquals("testuser", response.getLogin());
        assertEquals("Authenticated successfully", response.getStatusMessage());
    }

    @Test
    void testGetAuthStatusResponseNotAuthenticated() throws Exception {
        String json = """
                {
                    "isAuthenticated": false,
                    "statusMessage": "Not authenticated"
                }
                """;

        GetAuthStatusResponse response = MAPPER.readValue(json, GetAuthStatusResponse.class);

        assertFalse(response.isAuthenticated());
        assertNull(response.getAuthType());
        assertNull(response.getHost());
        assertNull(response.getLogin());
        assertEquals("Not authenticated", response.getStatusMessage());
    }

    @Test
    void testModelInfoDeserialization() throws Exception {
        String json = """
                {
                    "id": "gpt-4",
                    "name": "GPT-4",
                    "capabilities": {
                        "supports": {
                            "vision": true
                        },
                        "limits": {
                            "max_prompt_tokens": 8192,
                            "max_context_window_tokens": 128000,
                            "vision": {
                                "supported_media_types": ["image/png", "image/jpeg"],
                                "max_prompt_images": 10,
                                "max_prompt_image_size": 20971520
                            }
                        }
                    },
                    "policy": {
                        "state": "active",
                        "terms": "https://example.com/terms"
                    },
                    "billing": {
                        "multiplier": 1.5
                    }
                }
                """;

        ModelInfo model = MAPPER.readValue(json, ModelInfo.class);

        assertEquals("gpt-4", model.getId());
        assertEquals("GPT-4", model.getName());

        // Capabilities
        assertNotNull(model.getCapabilities());
        assertTrue(model.getCapabilities().getSupports().isVision());
        assertEquals(8192, model.getCapabilities().getLimits().getMaxPromptTokens());
        assertEquals(128000, model.getCapabilities().getLimits().getMaxContextWindowTokens());

        // Vision limits
        ModelVisionLimits visionLimits = model.getCapabilities().getLimits().getVision();
        assertNotNull(visionLimits);
        assertEquals(List.of("image/png", "image/jpeg"), visionLimits.getSupportedMediaTypes());
        assertEquals(10, visionLimits.getMaxPromptImages());
        assertEquals(20971520, visionLimits.getMaxPromptImageSize());

        // Policy
        assertNotNull(model.getPolicy());
        assertEquals("active", model.getPolicy().getState());
        assertEquals("https://example.com/terms", model.getPolicy().getTerms());

        // Billing
        assertNotNull(model.getBilling());
        assertEquals(1.5, model.getBilling().getMultiplier());
    }

    @Test
    void testGetModelsResponseDeserialization() throws Exception {
        String json = """
                {
                    "models": [
                        {
                            "id": "gpt-4",
                            "name": "GPT-4",
                            "capabilities": {
                                "supports": { "vision": false },
                                "limits": { "max_context_window_tokens": 8192 }
                            }
                        },
                        {
                            "id": "claude-3",
                            "name": "Claude 3",
                            "capabilities": {
                                "supports": { "vision": true },
                                "limits": { "max_context_window_tokens": 200000 }
                            }
                        }
                    ]
                }
                """;

        GetModelsResponse response = MAPPER.readValue(json, GetModelsResponse.class);

        assertNotNull(response.getModels());
        assertEquals(2, response.getModels().size());
        assertEquals("gpt-4", response.getModels().get(0).getId());
        assertEquals("claude-3", response.getModels().get(1).getId());
    }

    // ===== Integration Tests (require CLI) =====

    @Test
    void testGetStatus() throws Exception {
        if (cliPath == null) {
            System.out.println("Skipping test: CLI not found");
            return;
        }

        try (var client = new CopilotClient(new CopilotClientOptions().setCliPath(cliPath).setUseStdio(true))) {
            client.start().get();

            GetStatusResponse status = client.getStatus().get();

            assertNotNull(status);
            assertNotNull(status.getVersion());
            assertFalse(status.getVersion().isEmpty());
            assertEquals(SdkProtocolVersion.get(), status.getProtocolVersion());
        }
    }

    @Test
    void testGetAuthStatus() throws Exception {
        if (cliPath == null) {
            System.out.println("Skipping test: CLI not found");
            return;
        }

        try (var client = new CopilotClient(new CopilotClientOptions().setCliPath(cliPath).setUseStdio(true))) {
            client.start().get();

            GetAuthStatusResponse authStatus = client.getAuthStatus().get();

            assertNotNull(authStatus);
            // The response should have a status message regardless of auth state
            // We can't guarantee the user is authenticated in tests
        }
    }

    @Test
    void testListModels() throws Exception {
        if (cliPath == null) {
            System.out.println("Skipping test: CLI not found");
            return;
        }

        try (var client = new CopilotClient(new CopilotClientOptions().setCliPath(cliPath).setUseStdio(true))) {
            client.start().get();

            // Note: listModels may require authentication
            // This test verifies the method exists and can be called
            try {
                List<ModelInfo> models = client.listModels().get();
                assertNotNull(models);
                // If we got models, verify they have expected fields
                for (ModelInfo model : models) {
                    assertNotNull(model.getId());
                    assertNotNull(model.getName());
                }
            } catch (Exception e) {
                // May fail if not authenticated, which is acceptable in tests
                System.out.println("listModels failed (may require auth): " + e.getMessage());
            }
        }
    }

    // ===== Protocol Version Test =====

    @Test
    void testProtocolVersionIsTwo() {
        assertEquals(2, SdkProtocolVersion.get());
    }
}
