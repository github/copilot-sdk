/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.ToolDefinition;

/**
 * Tests for custom tools functionality.
 *
 * <p>
 * These tests use the shared CapiProxy infrastructure for deterministic API
 * response replay. Snapshots are stored in test/snapshots/tools/.
 * </p>
 */
public class ToolsTest {

    private static E2ETestContext ctx;

    @BeforeAll
    static void setup() throws Exception {
        ctx = E2ETestContext.create();
    }

    @AfterAll
    static void teardown() throws Exception {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Test
    void testInvokesBuiltInTools(TestInfo testInfo) throws Exception {
        ctx.configureForTest("tools", "invokes_built_in_tools");

        // Write a test file
        Path readmeFile = ctx.getWorkDir().resolve("README.md");
        Files.writeString(readmeFile, "# ELIZA, the only chatbot you'll ever need");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession().get();

            AssistantMessageEvent response = session
                    .sendAndWait(
                            new MessageOptions().setPrompt("What's the first line of README.md in this directory?"))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().getContent().contains("ELIZA"),
                    "Response should contain ELIZA: " + response.getData().getContent());

            session.close();
        }
    }

    @Test
    void testInvokesCustomTool(TestInfo testInfo) throws Exception {
        ctx.configureForTest("tools", "invokes_custom_tool");

        // Define a simple encrypt_string tool
        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> inputProp = new HashMap<>();
        inputProp.put("type", "string");
        inputProp.put("description", "String to encrypt");
        properties.put("input", inputProp);
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", List.of("input"));

        ToolDefinition encryptTool = ToolDefinition.create("encrypt_string", "Encrypts a string", parameters,
                (invocation) -> {
                    JsonNode argsNode = (JsonNode) invocation.getArguments();
                    String input = argsNode.get("input").asText();
                    return CompletableFuture.completedFuture(input.toUpperCase());
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setTools(List.of(encryptTool))).get();

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt("Use encrypt_string to encrypt this string: Hello"))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().getContent().contains("HELLO"),
                    "Response should contain HELLO: " + response.getData().getContent());

            session.close();
        }
    }

    @Test
    void testHandlesToolCallingErrors(TestInfo testInfo) throws Exception {
        ctx.configureForTest("tools", "handles_tool_calling_errors");

        // Define a tool that throws an error
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", new HashMap<>());

        ToolDefinition errorTool = ToolDefinition.create("get_user_location", "Gets the user's location", parameters,
                (invocation) -> {
                    CompletableFuture<Object> future = new CompletableFuture<>();
                    future.completeExceptionally(new RuntimeException("Melbourne"));
                    return future;
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setTools(List.of(errorTool))).get();

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions()
                            .setPrompt("What is my location? If you can't find out, just say 'unknown'."))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            // The error message should NOT be exposed to the assistant
            String content = response.getData().getContent().toLowerCase();
            assertFalse(content.contains("melbourne"), "Error details should not be exposed in response: " + content);
            assertTrue(content.contains("unknown") || content.contains("unable") || content.contains("cannot"),
                    "Response should indicate inability to get location: " + content);

            session.close();
        }
    }

    @Test
    void testCanReceiveAndReturnComplexTypes(TestInfo testInfo) throws Exception {
        ctx.configureForTest("tools", "can_receive_and_return_complex_types");

        // Define a db_query tool with complex parameter and return types
        Map<String, Object> querySchema = new HashMap<>();
        Map<String, Object> queryProps = new HashMap<>();
        queryProps.put("table", Map.of("type", "string"));
        queryProps.put("ids", Map.of("type", "array", "items", Map.of("type", "integer")));
        queryProps.put("sortAscending", Map.of("type", "boolean"));
        querySchema.put("type", "object");
        querySchema.put("properties", queryProps);
        querySchema.put("required", List.of("table", "ids", "sortAscending"));

        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        properties.put("query", querySchema);
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", List.of("query"));

        ToolDefinition dbQueryTool = ToolDefinition.create("db_query", "Performs a database query", parameters,
                (invocation) -> {
                    JsonNode argsNode = (JsonNode) invocation.getArguments();
                    JsonNode queryNode = argsNode.get("query");

                    assertEquals("cities", queryNode.get("table").asText());

                    // Return complex data structure
                    List<Map<String, Object>> results = List.of(
                            Map.of("countryId", 19, "cityName", "Passos", "population", 135460),
                            Map.of("countryId", 12, "cityName", "San Lorenzo", "population", 204356));

                    return CompletableFuture.completedFuture(results);
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setTools(List.of(dbQueryTool))).get();

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt(
                            "Perform a DB query for the 'cities' table using IDs 12 and 19, sorting ascending. "
                                    + "Reply only with lines of the form: [cityname] [population]"))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            String content = response.getData().getContent();
            assertTrue(content.contains("Passos"), "Response should contain Passos: " + content);
            assertTrue(content.contains("San Lorenzo"), "Response should contain San Lorenzo: " + content);

            session.close();
        }
    }
}
