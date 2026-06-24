/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.copilot.AllowCopilotExperimental;
import com.github.copilot.rpc.fixtures.ArgCoercionTools;
import com.github.copilot.rpc.fixtures.DateTimeTools;
import com.github.copilot.rpc.fixtures.DefaultValueTools;
import com.github.copilot.rpc.fixtures.MultiReturnTools;
import com.github.copilot.rpc.fixtures.OverrideTools;
import com.github.copilot.rpc.fixtures.SimpleTools;

/**
 * End-to-end tests for {@link ToolDefinition#fromObject(Object)}.
 * <p>
 * The annotation processor generates {@code $$CopilotToolMeta} companion
 * classes for the fixture classes during test compilation.
 */
@AllowCopilotExperimental
class ToolDefinitionFromObjectTest {

    // ── Test 1: Basic end-to-end ────────────────────────────────────────────────

    @Test
    void fromObject_returnsCorrectNumberOfTools() {
        var tools = ToolDefinition.fromObject(new SimpleTools());
        assertEquals(2, tools.size());
    }

    @Test
    void fromObject_toolNamesAndDescriptions() {
        var tools = ToolDefinition.fromObject(new SimpleTools());
        var tool1 = findTool(tools, "greet_user");
        assertNotNull(tool1);
        assertEquals("Greets a user by name", tool1.description());

        var tool2 = findTool(tools, "add_numbers");
        assertNotNull(tool2);
        assertEquals("Adds two numbers together", tool2.description());
    }

    @Test
    void fromObject_toolParameterSchema() {
        var tools = ToolDefinition.fromObject(new SimpleTools());
        var tool = findTool(tools, "greet_user");
        assertNotNull(tool);
        @SuppressWarnings("unchecked")
        var schema = (Map<String, Object>) tool.parameters();
        assertEquals("object", schema.get("type"));
        @SuppressWarnings("unchecked")
        var properties = (Map<String, Object>) schema.get("properties");
        assertTrue(properties.containsKey("name"));
        @SuppressWarnings("unchecked")
        var required = (List<String>) schema.get("required");
        assertTrue(required.contains("name"));
    }

    @Test
    void fromObject_handlerInvocation() throws Exception {
        var instance = new SimpleTools();
        var tools = ToolDefinition.fromObject(instance);
        var tool = findTool(tools, "greet_user");
        assertNotNull(tool);

        var result = tool.handler().invoke(createInvocation("greet_user", Map.of("name", "Alice"))).get();
        assertEquals("Hello, Alice!", result);
    }

    // ── Test 2: Handler return type patterns ────────────────────────────────────

    @Test
    void fromObject_stringReturn() throws Exception {
        var tools = ToolDefinition.fromObject(new MultiReturnTools());
        var tool = findTool(tools, "string_method");
        assertNotNull(tool);
        var result = tool.handler().invoke(createInvocation("string_method", Map.of())).get();
        assertEquals("hello", result);
    }

    @Test
    void fromObject_voidReturn() throws Exception {
        var tools = ToolDefinition.fromObject(new MultiReturnTools());
        var tool = findTool(tools, "void_method");
        assertNotNull(tool);
        var result = tool.handler().invoke(createInvocation("void_method", Map.of())).get();
        assertEquals("Success", result);
    }

    @Test
    void fromObject_asyncReturn() throws Exception {
        var tools = ToolDefinition.fromObject(new MultiReturnTools());
        var tool = findTool(tools, "async_method");
        assertNotNull(tool);
        var result = tool.handler().invoke(createInvocation("async_method", Map.of())).get();
        assertEquals("async result", result);
    }

    // ── Test 3: Argument coercion ───────────────────────────────────────────────

    @Test
    void fromObject_argumentCoercion() throws Exception {
        var instance = new ArgCoercionTools();
        var tools = ToolDefinition.fromObject(instance);
        var tool = findTool(tools, "mixed_args");
        assertNotNull(tool);

        var result = tool.handler().invoke(
                createInvocation("mixed_args", Map.of("text", "hello", "count", 5, "flag", true, "color", "RED")))
                .get();
        assertEquals("hello-5-true-RED", result);
    }

    // ── Test 4: Default value ───────────────────────────────────────────────────

    @Test
    void fromObject_defaultValue() throws Exception {
        var instance = new DefaultValueTools();
        var tools = ToolDefinition.fromObject(instance);
        var tool = findTool(tools, "with_default");
        assertNotNull(tool);

        // Omit "count" key — should use default value 42
        var result = tool.handler().invoke(createInvocation("with_default", Map.of("label", "test"))).get();
        assertEquals("test:42", result);
    }

    // ── Test 5: Error case — missing generated class ────────────────────────────

    @Test
    void fromObject_throwsOnMissingMetaClass() {
        // A class that was never processed by CopilotToolProcessor
        var ex = assertThrows(IllegalStateException.class, () -> ToolDefinition.fromObject("a plain String"));
        assertTrue(ex.getMessage().contains("not found"));
        assertTrue(ex.getMessage().contains("CopilotToolProcessor"));
    }

    // ── Test 5b: fromClass rejects instance methods ─────────────────────────────

    @Test
    void fromClass_throwsOnInstanceMethods() {
        // SimpleTools has instance (non-static) @CopilotTool methods
        var ex = assertThrows(IllegalArgumentException.class, () -> ToolDefinition.fromClass(SimpleTools.class));
        assertTrue(ex.getMessage().contains("fromClass()"));
        assertTrue(ex.getMessage().contains("static"));
        assertTrue(ex.getMessage().contains("fromObject"));
    }

    // ── Test 6: java.time argument ──────────────────────────────────────────────

    @Test
    void fromObject_javaTimeArgument() throws Exception {
        var instance = new DateTimeTools();
        var tools = ToolDefinition.fromObject(instance);
        var tool = findTool(tools, "schedule_event");
        assertNotNull(tool);

        var result = tool.handler().invoke(createInvocation("schedule_event", Map.of("when", "2024-06-15T10:30:00")))
                .get();
        assertEquals("Scheduled at 2024-06-15T10:30", result);
    }

    // ── Test 7: Override tool ────────────────────────────────────────────────────

    @Test
    void fromObject_overrideTool() {
        var tools = ToolDefinition.fromObject(new OverrideTools());
        var tool = findTool(tools, "grep");
        assertNotNull(tool);
        assertEquals(Boolean.TRUE, tool.overridesBuiltInTool());
    }

    // ── Test 8: ToolDefer.NONE → null mapping (defer absent from JSON) ──────────

    @Test
    void fromObject_deferNone_absentFromJson() throws Exception {
        var tools = ToolDefinition.fromObject(new SimpleTools());
        var tool = findTool(tools, "greet_user");
        assertNotNull(tool);
        // The defer field should be null (NONE maps to null)
        assertNull(tool.defer());

        // Serialize to JSON and verify "defer" key is absent
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

        String json = mapper.writeValueAsString(tool);
        assertFalse(json.contains("\"defer\""), "defer key should be absent from JSON, got: " + json);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private static ToolDefinition findTool(List<ToolDefinition> tools, String name) {
        return tools.stream().filter(t -> name.equals(t.name())).findFirst().orElse(null);
    }

    private static ToolInvocation createInvocation(String toolName, Map<String, ?> args) {
        ObjectNode argsNode = JsonNodeFactory.instance.objectNode();
        ObjectMapper mapper = new ObjectMapper();
        argsNode.setAll((ObjectNode) mapper.valueToTree(args));
        return new ToolInvocation().setToolName(toolName).setArguments(argsNode);
    }
}
