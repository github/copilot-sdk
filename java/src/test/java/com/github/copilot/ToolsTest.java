/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.PermissionRequest;
import com.github.copilot.rpc.PermissionRequestResult;
import com.github.copilot.rpc.PermissionRequestResultKind;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.ToolDefinition;

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

    /**
     * Verifies that built-in tools are invoked correctly.
     *
     * @see Snapshot: tools/invokes_built_in_tools
     */
    @Test
    void testInvokesBuiltInTools(TestInfo testInfo) throws Exception {
        ctx.configureForTest("tools", "invokes_built_in_tools");

        // Write a test file
        Path readmeFile = ctx.getWorkDir().resolve("README.md");
        Files.writeString(readmeFile, "# ELIZA, the only chatbot you'll ever need");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            AssistantMessageEvent response = session
                    .sendAndWait(
                            new MessageOptions().setPrompt("What's the first line of README.md in this directory?"))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains("ELIZA"),
                    "Response should contain ELIZA: " + response.getData().content());

            session.close();
        }
    }

    /**
     * Verifies that custom tools are invoked correctly.
     *
     * @see Snapshot: tools/invokes_custom_tool
     */
    @Test
    void testInvokesCustomTool(TestInfo testInfo) throws Exception {
        ctx.configureForTest("tools", "invokes_custom_tool");

        // Define a simple encrypt_string tool
        var parameters = new HashMap<String, Object>();
        var properties = new HashMap<String, Object>();
        var inputProp = new HashMap<String, Object>();
        inputProp.put("type", "string");
        inputProp.put("description", "String to encrypt");
        properties.put("input", inputProp);
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", List.of("input"));

        ToolDefinition encryptTool = ToolDefinition.create("encrypt_string", "Encrypts a string", parameters,
                (invocation) -> {
                    Map<String, Object> args = invocation.getArguments();
                    String input = (String) args.get("input");
                    return CompletableFuture.completedFuture(input.toUpperCase());
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setTools(List.of(encryptTool))
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt("Use encrypt_string to encrypt this string: Hello"))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains("HELLO"),
                    "Response should contain HELLO: " + response.getData().content());

            session.close();
        }
    }

    /**
     * Verifies that tool calling errors are handled gracefully.
     *
     * @see Snapshot: tools/handles_tool_calling_errors
     */
    @Test
    void testHandlesToolCallingErrors(TestInfo testInfo) throws Exception {
        ctx.configureForTest("tools", "handles_tool_calling_errors");

        // Define a tool that throws an error
        var parameters = new HashMap<String, Object>();
        parameters.put("type", "object");
        parameters.put("properties", new HashMap<>());

        ToolDefinition errorTool = ToolDefinition.create("get_user_location", "Gets the user's location", parameters,
                (invocation) -> {
                    CompletableFuture<Object> future = new CompletableFuture<>();
                    future.completeExceptionally(new RuntimeException("Melbourne"));
                    return future;
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setTools(List.of(errorTool))
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions()
                            .setPrompt("What is my location? If you can't find out, just say 'unknown'."))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            // The error message should NOT be exposed to the assistant
            String content = response.getData().content().toLowerCase();
            assertFalse(content.contains("melbourne"), "Error details should not be exposed in response: " + content);
            assertTrue(content.contains("unknown") || content.contains("unable") || content.contains("cannot"),
                    "Response should indicate inability to get location: " + content);

            session.close();
        }
    }

    /**
     * Verifies that tools can receive and return complex types.
     *
     * @see Snapshot: tools/can_receive_and_return_complex_types
     */
    @Test
    void testCanReceiveAndReturnComplexTypes(TestInfo testInfo) throws Exception {
        ctx.configureForTest("tools", "can_receive_and_return_complex_types");

        // Define a db_query tool with complex parameter and return types
        var querySchema = new HashMap<String, Object>();
        var queryProps = new HashMap<String, Object>();
        queryProps.put("table", Map.of("type", "string"));
        queryProps.put("ids", Map.of("type", "array", "items", Map.of("type", "integer")));
        queryProps.put("sortAscending", Map.of("type", "boolean"));
        querySchema.put("type", "object");
        querySchema.put("properties", queryProps);
        querySchema.put("required", List.of("table", "ids", "sortAscending"));

        var parameters = new HashMap<String, Object>();
        var properties = new HashMap<String, Object>();
        properties.put("query", querySchema);
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", List.of("query"));

        ToolDefinition dbQueryTool = ToolDefinition.create("db_query", "Performs a database query", parameters,
                (invocation) -> {
                    Map<String, Object> args = invocation.getArguments();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> query = (Map<String, Object>) args.get("query");

                    assertEquals("cities", query.get("table"));

                    // Return complex data structure
                    List<Map<String, Object>> results = List.of(
                            Map.of("countryId", 19, "cityName", "Passos", "population", 135460),
                            Map.of("countryId", 12, "cityName", "San Lorenzo", "population", 204356));

                    return CompletableFuture.completedFuture(results);
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setTools(List.of(dbQueryTool))
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt(
                            "Perform a DB query for the 'cities' table using IDs 12 and 19, sorting ascending. "
                                    + "Reply only with lines of the form: [cityname] [population]"))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            String content = response.getData().content();
            assertTrue(content.contains("Passos"), "Response should contain Passos: " + content);
            assertTrue(content.contains("San Lorenzo"), "Response should contain San Lorenzo: " + content);

            session.close();
        }
    }

    /**
     * Verifies that a custom tool is invoked with the permission handler being
     * called and can inspect the permission request kind.
     *
     * @see Snapshot: tools/invokes_custom_tool_with_permission_handler
     */
    @Test
    void testInvokesCustomToolWithPermissionHandler(TestInfo testInfo) throws Exception {
        ctx.configureForTest("tools", "invokes_custom_tool_with_permission_handler");

        var permissionRequests = new ArrayList<PermissionRequest>();

        var parameters = new HashMap<String, Object>();
        parameters.put("type", "object");
        var props = new HashMap<String, Object>();
        props.put("input", Map.of("type", "string"));
        parameters.put("properties", props);
        parameters.put("required", List.of("input"));

        ToolDefinition encryptTool = ToolDefinition.create("encrypt_string", "Encrypts a string", parameters,
                (invocation) -> {
                    Map<String, Object> args = invocation.getArguments();
                    String input = (String) args.get("input");
                    return CompletableFuture.completedFuture(input.toUpperCase());
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(
                    new SessionConfig().setTools(List.of(encryptTool)).setOnPermissionRequest((request, invocation) -> {
                        permissionRequests.add(request);
                        return PermissionHandler.APPROVE_ALL.handle(request, invocation);
                    })).get();

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt("Use encrypt_string to encrypt this string: Hello"))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains("HELLO"),
                    "Response should contain HELLO: " + response.getData().content());

            // Should have received a custom-tool permission request
            boolean hasCustomToolRequest = permissionRequests.stream()
                    .anyMatch(req -> "custom-tool".equals(req.getKind()));
            assertTrue(hasCustomToolRequest, "Should have received a custom-tool permission request");

            session.close();
        }
    }

    /**
     * Verifies that a custom tool is denied when the permission handler denies it.
     *
     * @see Snapshot: tools/denies_custom_tool_when_permission_denied
     */
    @Test
    void testDeniesCustomToolWhenPermissionDenied(TestInfo testInfo) throws Exception {
        ctx.configureForTest("tools", "denies_custom_tool_when_permission_denied");

        final boolean[] toolHandlerCalled = {false};

        var parameters = new HashMap<String, Object>();
        parameters.put("type", "object");
        var props = new HashMap<String, Object>();
        props.put("input", Map.of("type", "string"));
        parameters.put("properties", props);
        parameters.put("required", List.of("input"));

        ToolDefinition encryptTool = ToolDefinition.create("encrypt_string", "Encrypts a string", parameters,
                (invocation) -> {
                    toolHandlerCalled[0] = true;
                    Map<String, Object> args = invocation.getArguments();
                    String input = (String) args.get("input");
                    return CompletableFuture.completedFuture(input.toUpperCase());
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setTools(List.of(encryptTool))
                            .setOnPermissionRequest((request, invocation) -> CompletableFuture.completedFuture(
                                    new PermissionRequestResult().setKind(PermissionRequestResultKind.REJECTED))))
                    .get();

            session.sendAndWait(new MessageOptions().setPrompt("Use encrypt_string to encrypt this string: Hello"))
                    .get(60, TimeUnit.SECONDS);

            // The tool handler should NOT have been called since permission was denied
            assertFalse(toolHandlerCalled[0], "Tool handler should not be called when permission is denied");

            session.close();
        }
    }

    /**
     * Verifies that a custom tool can override a built-in CLI tool with the same
     * name when {@code overridesBuiltInTool} is set to {@code true}.
     *
     * @see Snapshot: tools/overrides_built_in_tool_with_custom_tool
     */
    @Test
    void testOverridesBuiltInToolWithCustomTool() throws Exception {
        ctx.configureForTest("tools", "overrides_built_in_tool_with_custom_tool");

        var parameters = new HashMap<String, Object>();
        var properties = new HashMap<String, Object>();
        properties.put("query", Map.of("type", "string", "description", "Search query"));
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", List.of("query"));

        ToolDefinition customGrep = ToolDefinition.createOverride("grep", "A custom grep implementation", parameters,
                (invocation) -> {
                    Map<String, Object> args = invocation.getArguments();
                    String query = (String) args.get("query");
                    return CompletableFuture.completedFuture("CUSTOM_GREP_RESULT: " + query);
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setTools(List.of(customGrep))
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt("Use grep to search for the word 'hello'"))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains("CUSTOM_GREP_RESULT"),
                    "Response should contain CUSTOM_GREP_RESULT: " + response.getData().content());

            session.close();
        }
    }

    /**
     * Verifies that the model can call multiple custom tools in parallel within a
     * single turn.
     *
     * @see Snapshot:
     *      tools/should_execute_multiple_custom_tools_in_parallel_single_turn
     */
    @Test
    void testShouldExecuteMultipleCustomToolsInParallelSingleTurn() throws Exception {
        ctx.configureForTest("tools", "should_execute_multiple_custom_tools_in_parallel_single_turn");

        var toolACalled = new CompletableFuture<String>();
        var toolBCalled = new CompletableFuture<String>();

        Map<String, Object> cityParams = Map.of("type", "object", "properties",
                Map.of("city", Map.of("type", "string", "description", "City name")), "required", List.of("city"));
        Map<String, Object> countryParams = Map.of("type", "object", "properties",
                Map.of("country", Map.of("type", "string", "description", "Country name")), "required",
                List.of("country"));

        ToolDefinition lookupCity = ToolDefinition.create("lookup_city", "Looks up city information", cityParams,
                (invocation) -> {
                    String city = (String) invocation.getArguments().get("city");
                    toolACalled.complete(city);
                    return CompletableFuture.completedFuture("CITY_" + city.toUpperCase());
                });

        ToolDefinition lookupCountry = ToolDefinition.create("lookup_country", "Looks up country information",
                countryParams, (invocation) -> {
                    String country = (String) invocation.getArguments().get("country");
                    toolBCalled.complete(country);
                    return CompletableFuture.completedFuture("COUNTRY_" + country.toUpperCase());
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig()
                    .setTools(List.of(lookupCity, lookupCountry)).setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get();

            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt(
                    "Use lookup_city with 'Paris' and lookup_country with 'France' at the same time, then combine both results in your reply."))
                    .get(60, TimeUnit.SECONDS);

            // Both tools should have been called
            assertEquals("Paris", toolACalled.get(10, TimeUnit.SECONDS));
            assertEquals("France", toolBCalled.get(10, TimeUnit.SECONDS));

            assertNotNull(response);
            String content = response.getData().content();
            assertTrue(content.contains("CITY_PARIS"), "Response should contain CITY_PARIS: " + content);
            assertTrue(content.contains("COUNTRY_FRANCE"), "Response should contain COUNTRY_FRANCE: " + content);

            session.close();
        }
    }

    /**
     * Verifies that excludedTools are respected even when also listed in
     * availableTools.
     *
     * @see Snapshot: tools/should_respect_availabletools_and_excludedtools_combined
     */
    @Test
    void testShouldRespectAvailableToolsAndExcludedToolsCombined() throws Exception {
        ctx.configureForTest("tools", "should_respect_availabletools_and_excludedtools_combined");

        var excludedToolCalled = new boolean[]{false};

        Map<String, Object> inputParams = Map.of("type", "object", "properties",
                Map.of("input", Map.of("type", "string", "description", "Input value")), "required", List.of("input"));

        ToolDefinition allowedTool = ToolDefinition.create("allowed_tool", "An allowed tool", inputParams,
                (invocation) -> {
                    String input = (String) invocation.getArguments().get("input");
                    return CompletableFuture.completedFuture("ALLOWED_" + input.toUpperCase());
                });

        ToolDefinition excludedTool = ToolDefinition.create("excluded_tool", "A tool that should be excluded",
                inputParams, (invocation) -> {
                    excludedToolCalled[0] = true;
                    String input = (String) invocation.getArguments().get("input");
                    return CompletableFuture.completedFuture("EXCLUDED_" + input.toUpperCase());
                });

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig()
                    .setTools(List.of(allowedTool, excludedTool))
                    .setAvailableTools(List.of("allowed_tool", "excluded_tool"))
                    .setExcludedTools(List.of("excluded_tool")).setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get();

            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions()
                            .setPrompt("Use the allowed_tool with input 'test'. Do NOT use excluded_tool."))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains("ALLOWED_TEST"),
                    "Response should contain ALLOWED_TEST: " + response.getData().content());
            assertFalse(excludedToolCalled[0], "Excluded tool should not have been called");

            session.close();
        }
    }
}
