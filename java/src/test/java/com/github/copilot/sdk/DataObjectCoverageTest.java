/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.sdk.json.CustomAgentConfig;
import com.github.copilot.sdk.json.GetForegroundSessionResponse;
import com.github.copilot.sdk.json.McpHttpServerConfig;
import com.github.copilot.sdk.json.McpStdioServerConfig;
import com.github.copilot.sdk.json.ModelCapabilitiesOverride;
import com.github.copilot.sdk.json.PermissionRequest;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.PostToolUseHookInput;
import com.github.copilot.sdk.json.PostToolUseHookOutput;
import com.github.copilot.sdk.json.PreToolUseHookInput;
import com.github.copilot.sdk.json.PreToolUseHookOutput;
import com.github.copilot.sdk.json.SectionOverride;
import com.github.copilot.sdk.json.SetForegroundSessionRequest;
import com.github.copilot.sdk.json.SetForegroundSessionResponse;
import com.github.copilot.sdk.json.ToolBinaryResult;
import com.github.copilot.sdk.json.ToolResultObject;

/**
 * Unit tests for various data transfer objects and record types that were
 * missing coverage, including hook output factory methods, record constructors,
 * and getters for hook inputs.
 */
class DataObjectCoverageTest {

    // ===== PreToolUseHookOutput factory methods =====

    @Test
    void preToolUseHookOutputDenyWithReason() {
        var output = PreToolUseHookOutput.deny("Security policy violation");
        assertEquals("deny", output.permissionDecision());
        assertEquals("Security policy violation", output.permissionDecisionReason());
        assertNull(output.modifiedArgs());
    }

    @Test
    void preToolUseHookOutputAsk() {
        var output = PreToolUseHookOutput.ask();
        assertEquals("ask", output.permissionDecision());
        assertNull(output.permissionDecisionReason());
    }

    @Test
    void preToolUseHookOutputWithModifiedArgs() {
        ObjectNode args = JsonNodeFactory.instance.objectNode();
        args.put("path", "/safe/path");

        var output = PreToolUseHookOutput.withModifiedArgs("allow", args);
        assertEquals("allow", output.permissionDecision());
        assertEquals(args, output.modifiedArgs());
    }

    // ===== PostToolUseHookOutput record =====

    @Test
    void postToolUseHookOutputRecord() {
        var output = new PostToolUseHookOutput(null, "Extra context", false);
        assertNull(output.modifiedResult());
        assertEquals("Extra context", output.additionalContext());
        assertFalse(output.suppressOutput());
    }

    // ===== ToolBinaryResult record =====

    @Test
    void toolBinaryResultRecord() {
        var result = new ToolBinaryResult("base64data==", "image/png", "image", "A chart");
        assertEquals("base64data==", result.data());
        assertEquals("image/png", result.mimeType());
        assertEquals("image", result.type());
        assertEquals("A chart", result.description());
    }

    // ===== GetForegroundSessionResponse record =====

    @Test
    void getForegroundSessionResponseRecord() {
        var response = new GetForegroundSessionResponse("session-123", "/home/user/project");
        assertEquals("session-123", response.sessionId());
        assertEquals("/home/user/project", response.workspacePath());
    }

    // ===== SetForegroundSessionRequest record =====

    @Test
    void setForegroundSessionRequestRecord() {
        var request = new SetForegroundSessionRequest("session-123");
        assertEquals("session-123", request.sessionId());
    }

    // ===== SetForegroundSessionResponse record =====

    @Test
    void setForegroundSessionResponseRecord() {
        var successResponse = new SetForegroundSessionResponse(true, null);
        assertTrue(successResponse.success());
        assertNull(successResponse.error());

        var errorResponse = new SetForegroundSessionResponse(false, "Session not found");
        assertFalse(errorResponse.success());
        assertEquals("Session not found", errorResponse.error());
    }

    // ===== ToolResultObject factory methods =====

    @Test
    void toolResultObjectErrorWithTextAndError() {
        var result = ToolResultObject.error("partial output", "File not found");
        assertEquals("error", result.resultType());
        assertEquals("partial output", result.textResultForLlm());
        assertEquals("File not found", result.error());
    }

    @Test
    void toolResultObjectFailure() {
        var result = ToolResultObject.failure("Tool unavailable", "Unknown tool");
        assertEquals("failure", result.resultType());
        assertEquals("Tool unavailable", result.textResultForLlm());
        assertEquals("Unknown tool", result.error());
    }

    // ===== PermissionRequest additional setters =====

    @Test
    void permissionRequestSetExtensionData() {
        var req = new PermissionRequest();
        req.setExtensionData(java.util.Map.of("key", "value"));
        assertEquals("value", req.getExtensionData().get("key"));
    }

    // ===== SectionOverride setContent =====

    @Test
    void sectionOverrideSetContent() {
        var override = new SectionOverride();
        override.setContent("Custom content");
        assertEquals("Custom content", override.getContent());
    }

    // ===== PreToolUseHookInput getters =====

    @Test
    void preToolUseHookInputGetters() {
        var input = new PreToolUseHookInput();
        // Default values
        assertEquals(0L, input.getTimestamp());
        assertNull(input.getWorkingDirectory());
        assertNull(input.getToolArgs());
        assertNull(input.getSessionId());
    }

    @Test
    void preToolUseHookInputSessionIdRoundTrip() {
        var input = new PreToolUseHookInput();
        input.setSessionId("session-abc");
        assertEquals("session-abc", input.getSessionId());
    }

    // ===== PostToolUseHookInput getters =====

    @Test
    void postToolUseHookInputGetters() {
        var input = new PostToolUseHookInput();
        // Default values
        assertEquals(0L, input.getTimestamp());
        assertNull(input.getWorkingDirectory());
        assertNull(input.getToolArgs());
        assertNull(input.getSessionId());
    }

    @Test
    void postToolUseHookInputSessionIdRoundTrip() {
        var input = new PostToolUseHookInput();
        input.setSessionId("session-xyz");
        assertEquals("session-xyz", input.getSessionId());
    }

    // ===== CustomAgentConfig model field =====

    @Test
    void customAgentConfigModelGetterAndSetter() {
        var cfg = new CustomAgentConfig();
        assertNull(cfg.getModel());

        cfg.setModel("claude-haiku-4.5");
        assertEquals("claude-haiku-4.5", cfg.getModel());
    }

    @Test
    void customAgentConfigModelFluentChaining() {
        var cfg = new CustomAgentConfig().setName("reviewer").setModel("gpt-5").setDescription("Code reviewer");
        assertEquals("reviewer", cfg.getName());
        assertEquals("gpt-5", cfg.getModel());
        assertEquals("Code reviewer", cfg.getDescription());
    }

    @Test
    void customAgentConfigModelSerializationRoundTrip() throws Exception {
        var mapper = JsonRpcClient.getObjectMapper();
        var cfg = new CustomAgentConfig().setName("my-agent").setModel("claude-haiku-4.5");

        var json = mapper.writeValueAsString(cfg);
        assertTrue(json.contains("\"model\":\"claude-haiku-4.5\""));

        var deserialized = mapper.readValue(json, CustomAgentConfig.class);
        assertEquals("my-agent", deserialized.getName());
        assertEquals("claude-haiku-4.5", deserialized.getModel());
    }

    @Test
    void customAgentConfigModelOmittedWhenNull() throws Exception {
        var mapper = JsonRpcClient.getObjectMapper();
        var cfg = new CustomAgentConfig().setName("no-model-agent");

        var json = mapper.writeValueAsString(cfg);
        assertFalse(json.contains("\"model\""));
    }

    // ===== PermissionRequestResult setRules =====

    @Test
    void permissionRequestResultSetRules() {
        var result = new PermissionRequestResult().setKind("allow");
        var rules = new java.util.ArrayList<Object>();
        rules.add("bash:read");
        rules.add("bash:write");
        result.setRules(rules);
        assertEquals(2, result.getRules().size());
        assertEquals("bash:read", result.getRules().get(0));
    }

    @Test
    void mcpHttpServerConfigCoversGettersAndFluentSetters() {
        var headers = java.util.Map.of("Authorization", "Bearer token");
        var tools = java.util.List.of("*", "search");

        var cfg = new McpHttpServerConfig().setUrl("https://mcp.example.com/sse").setHeaders(headers).setTools(tools)
                .setTimeout(45);

        assertEquals("http", cfg.getType());
        assertEquals("https://mcp.example.com/sse", cfg.getUrl());
        assertEquals("Bearer token", cfg.getHeaders().get("Authorization"));
        assertEquals(tools, cfg.getTools());
        assertEquals(45, cfg.getTimeout());
    }

    @Test
    void mcpStdioServerConfigCoversGettersAndFluentSetters() {
        var args = java.util.List.of("-y", "@modelcontextprotocol/server-filesystem");
        var env = java.util.Map.of("DEBUG", "1");
        var tools = java.util.List.of("*");

        var cfg = new McpStdioServerConfig().setCommand("npx").setArgs(args).setEnv(env).setWorkingDirectory("/tmp")
                .setTools(tools).setTimeout(30);

        assertEquals("stdio", cfg.getType());
        assertEquals("npx", cfg.getCommand());
        assertEquals(args, cfg.getArgs());
        assertEquals("1", cfg.getEnv().get("DEBUG"));
        assertEquals("/tmp", cfg.getWorkingDirectory());
        assertEquals(tools, cfg.getTools());
        assertEquals(30, cfg.getTimeout());
    }

    @Test
    void modelCapabilitiesOverrideCoversNestedSupportsAndLimits() {
        var supports = new ModelCapabilitiesOverride.Supports().setVision(true).setReasoningEffort(false);
        var limits = new ModelCapabilitiesOverride.Limits().setMaxPromptTokens(2048).setMaxOutputTokens(512)
                .setMaxContextWindowTokens(8192);

        var override = new ModelCapabilitiesOverride().setSupports(supports).setLimits(limits);

        assertTrue(override.getSupports().getVision().orElse(false));
        assertFalse(override.getSupports().getReasoningEffort().orElse(true));
        assertEquals(2048, override.getLimits().getMaxPromptTokens().getAsInt());
        assertEquals(512, override.getLimits().getMaxOutputTokens().getAsInt());
        assertEquals(8192, override.getLimits().getMaxContextWindowTokens().getAsInt());
    }
}
