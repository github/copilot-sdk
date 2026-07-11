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
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.AllowCopilotExperimental;
import com.github.copilot.tool.Param;

/**
 * Unit tests for {@link ToolDefinition#from}, {@link ToolDefinition#fromAsync},
 * {@link ToolDefinition#fromWithToolInvocation}, and
 * {@link ToolDefinition#fromAsyncWithToolInvocation} lambda-tool factories,
 * plus the fluent option-modifier methods
 * ({@link ToolDefinition#overridesBuiltInTool},
 * {@link ToolDefinition#skipPermission}, {@link ToolDefinition#defer}).
 *
 * <p>
 * Tests are grouped by the Phase 4.4 contract:
 * <ol>
 * <li>Successful inline definitions for arities 0–2 (sync and async).</li>
 * <li>ToolInvocation context injection (sync and async).</li>
 * <li>Option flag propagation.</li>
 * <li>Required/default semantics.</li>
 * <li>Error and validation paths.</li>
 * <li>Schema structure.</li>
 * <li>Result formatting (String, null, non-String).</li>
 * <li>Argument coercion.</li>
 * </ol>
 */
@AllowCopilotExperimental
class ToolDefinitionLambdaTest {

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private static ToolInvocation invocationOf(Map<String, ?> args) {
        ObjectNode argsNode = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, ?> e : args.entrySet()) {
            Object v = e.getValue();
            if (v instanceof String s) {
                argsNode.put(e.getKey(), s);
            } else if (v instanceof Integer i) {
                argsNode.put(e.getKey(), i);
            } else if (v instanceof Long l) {
                argsNode.put(e.getKey(), l);
            } else if (v instanceof Double d) {
                argsNode.put(e.getKey(), d);
            } else if (v instanceof Boolean b) {
                argsNode.put(e.getKey(), b);
            } else if (v != null) {
                argsNode.put(e.getKey(), v.toString());
            }
        }
        return new ToolInvocation().setArguments(argsNode);
    }

    private static ToolInvocation invocationWithContext(String sessionId, String toolCallId, Map<String, ?> args) {
        return invocationOf(args).setSessionId(sessionId).setToolCallId(toolCallId);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> schemaOf(ToolDefinition tool) {
        return (Map<String, Object>) tool.parameters();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> propertiesOf(ToolDefinition tool) {
        return (Map<String, Object>) schemaOf(tool).get("properties");
    }

    @SuppressWarnings("unchecked")
    private static List<String> requiredOf(ToolDefinition tool) {
        return (List<String>) schemaOf(tool).get("required");
    }

    // ── Group 1: Successful inline definitions – arity 0, sync ───────────────────

    @Test
    void from_zeroArg_returnsNameAndDescription() {
        ToolDefinition tool = ToolDefinition.from("ping", "Returns pong", () -> "pong");
        assertEquals("ping", tool.name());
        assertEquals("Returns pong", tool.description());
    }

    @Test
    void from_zeroArg_invokesHandler() throws Exception {
        ToolDefinition tool = ToolDefinition.from("ping", "Returns pong", () -> "pong");
        Object result = tool.handler().invoke(invocationOf(Map.of())).get();
        assertEquals("pong", result);
    }

    @Test
    void from_zeroArg_emptySchema() {
        ToolDefinition tool = ToolDefinition.from("ping", "Returns pong", () -> "pong");
        assertTrue(propertiesOf(tool).isEmpty());
        assertTrue(requiredOf(tool).isEmpty());
    }

    // ── Group 1: Successful inline definitions – arity 1, sync ───────────────────

    @Test
    void from_oneArg_returnsNameAndDescription() {
        Param<String> nameParam = Param.of(String.class, "name", "The user's name");
        ToolDefinition tool = ToolDefinition.from("greet", "Greets a user", nameParam, n -> "Hello, " + n + "!");
        assertEquals("greet", tool.name());
        assertEquals("Greets a user", tool.description());
    }

    @Test
    void from_oneArg_invokesHandler() throws Exception {
        Param<String> nameParam = Param.of(String.class, "name", "The user's name");
        ToolDefinition tool = ToolDefinition.from("greet", "Greets a user", nameParam, n -> "Hello, " + n + "!");
        Object result = tool.handler().invoke(invocationOf(Map.of("name", "Alice"))).get();
        assertEquals("Hello, Alice!", result);
    }

    @Test
    void from_oneArg_schemaContainsParam() {
        Param<String> nameParam = Param.of(String.class, "name", "The user's name");
        ToolDefinition tool = ToolDefinition.from("greet", "Greets a user", nameParam, n -> "Hello, " + n + "!");
        assertTrue(propertiesOf(tool).containsKey("name"));
        assertTrue(requiredOf(tool).contains("name"));
    }

    // ── Group 1: Successful inline definitions – arity 2, sync ───────────────────

    @Test
    void from_twoArg_invokesHandler() throws Exception {
        Param<Integer> paramA = Param.of(Integer.class, "a", "First number");
        Param<Integer> paramB = Param.of(Integer.class, "b", "Second number");
        ToolDefinition tool = ToolDefinition.from("add", "Adds two integers", paramA, paramB,
                (a, b) -> String.valueOf(a + b));
        Object result = tool.handler().invoke(invocationOf(Map.of("a", 3, "b", 4))).get();
        assertEquals("7", result);
    }

    @Test
    void from_twoArg_schemaBothParamsPresent() {
        Param<Integer> paramA = Param.of(Integer.class, "a", "First");
        Param<Integer> paramB = Param.of(Integer.class, "b", "Second");
        ToolDefinition tool = ToolDefinition.from("add", "Adds two integers", paramA, paramB, (a, b) -> a + b);
        assertTrue(propertiesOf(tool).containsKey("a"));
        assertTrue(propertiesOf(tool).containsKey("b"));
        assertTrue(requiredOf(tool).contains("a"));
        assertTrue(requiredOf(tool).contains("b"));
    }

    // ── Group 2: Async handlers (fromAsync) ──────────────────────────────────────

    @Test
    void fromAsync_zeroArg_invokesHandler() throws Exception {
        ToolDefinition tool = ToolDefinition.fromAsync("ping_async", "Async ping",
                () -> CompletableFuture.completedFuture("pong"));
        Object result = tool.handler().invoke(invocationOf(Map.of())).get();
        assertEquals("pong", result);
    }

    @Test
    void fromAsync_oneArg_invokesHandler() throws Exception {
        Param<String> nameParam = Param.of(String.class, "name", "Name to greet");
        ToolDefinition tool = ToolDefinition.fromAsync("greet_async", "Async greet", nameParam,
                n -> CompletableFuture.completedFuture("Hi, " + n + "!"));
        Object result = tool.handler().invoke(invocationOf(Map.of("name", "Bob"))).get();
        assertEquals("Hi, Bob!", result);
    }

    @Test
    void fromAsync_twoArg_invokesHandler() throws Exception {
        Param<Integer> paramA = Param.of(Integer.class, "a", "Left operand");
        Param<Integer> paramB = Param.of(Integer.class, "b", "Right operand");
        ToolDefinition tool = ToolDefinition.fromAsync("add_async", "Async add", paramA, paramB,
                (a, b) -> CompletableFuture.completedFuture(String.valueOf(a + b)));
        Object result = tool.handler().invoke(invocationOf(Map.of("a", 10, "b", 5))).get();
        assertEquals("15", result);
    }

    // ── Group 3: ToolInvocation context injection (sync) ─────────────────────────

    @Test
    void fromWithToolInvocation_zeroArg_receivesContext() throws Exception {
        ToolDefinition tool = ToolDefinition.fromWithToolInvocation("ctx_sync", "Returns session id",
                inv -> "session=" + inv.getSessionId());
        Object result = tool.handler().invoke(invocationWithContext("sess-1", "call-1", Map.of())).get();
        assertEquals("session=sess-1", result);
    }

    @Test
    void fromWithToolInvocation_zeroArg_emptySchema() {
        ToolDefinition tool = ToolDefinition.fromWithToolInvocation("ctx_sync", "Returns session id",
                inv -> "session=" + inv.getSessionId());
        assertTrue(propertiesOf(tool).isEmpty());
        assertTrue(requiredOf(tool).isEmpty());
    }

    @Test
    void fromWithToolInvocation_oneArg_receivesArgAndContext() throws Exception {
        Param<String> phaseParam = Param.of(String.class, "phase", "Current phase");
        ToolDefinition tool = ToolDefinition.fromWithToolInvocation("report", "Report phase", phaseParam,
                (phase, inv) -> "phase=" + phase + ",callId=" + inv.getToolCallId());
        Object result = tool.handler().invoke(invocationWithContext("sess-2", "call-42", Map.of("phase", "analysis")))
                .get();
        assertEquals("phase=analysis,callId=call-42", result);
    }

    @Test
    void fromWithToolInvocation_oneArg_schemaExcludesInvocationParam() {
        Param<String> phaseParam = Param.of(String.class, "phase", "Current phase");
        ToolDefinition tool = ToolDefinition.fromWithToolInvocation("report", "Report phase", phaseParam,
                (phase, inv) -> phase);
        assertTrue(propertiesOf(tool).containsKey("phase"));
        assertFalse(propertiesOf(tool).containsKey("invocation"));
        assertEquals(List.of("phase"), requiredOf(tool));
    }

    // ── Group 4: Async ToolInvocation context injection ──────────────────────────

    @Test
    void fromAsyncWithToolInvocation_zeroArg_receivesContext() throws Exception {
        ToolDefinition tool = ToolDefinition.fromAsyncWithToolInvocation("ctx_async", "Async ctx",
                inv -> CompletableFuture.completedFuture("callId=" + inv.getToolCallId()));
        Object result = tool.handler().invoke(invocationWithContext("sess-3", "call-99", Map.of())).get();
        assertEquals("callId=call-99", result);
    }

    @Test
    void fromAsyncWithToolInvocation_oneArg_receivesArgAndContext() throws Exception {
        Param<String> phaseParam = Param.of(String.class, "phase", "Phase name");
        ToolDefinition tool = ToolDefinition.fromAsyncWithToolInvocation("report_async", "Async report", phaseParam,
                (phase, inv) -> CompletableFuture.completedFuture("phase=" + phase + ",sess=" + inv.getSessionId()));
        Object result = tool.handler().invoke(invocationWithContext("sess-4", "call-7", Map.of("phase", "planning")))
                .get();
        assertEquals("phase=planning,sess=sess-4", result);
    }

    // ── Group 5: Option flag propagation ─────────────────────────────────────────

    @Test
    void overridesBuiltInTool_setsFlag() {
        ToolDefinition base = ToolDefinition.from("grep", "Custom grep", () -> "ok");
        assertNull(base.overridesBuiltInTool());
        ToolDefinition withOverride = base.overridesBuiltInTool(true);
        assertEquals(Boolean.TRUE, withOverride.overridesBuiltInTool());
    }

    @Test
    void overridesBuiltInTool_doesNotMutateOriginal() {
        ToolDefinition base = ToolDefinition.from("grep", "Custom grep", () -> "ok");
        base.overridesBuiltInTool(true);
        assertNull(base.overridesBuiltInTool(), "original must remain unchanged");
    }

    @Test
    void skipPermission_setsFlag() {
        ToolDefinition base = ToolDefinition.from("read_file", "Reads a file", () -> "contents");
        assertNull(base.skipPermission());
        ToolDefinition withSkip = base.skipPermission(true);
        assertEquals(Boolean.TRUE, withSkip.skipPermission());
    }

    @Test
    void skipPermission_doesNotMutateOriginal() {
        ToolDefinition base = ToolDefinition.from("read_file", "Reads a file", () -> "contents");
        base.skipPermission(true);
        assertNull(base.skipPermission(), "original must remain unchanged");
    }

    @Test
    void defer_setsAutoMode() {
        ToolDefinition base = ToolDefinition.from("search", "Searches things", () -> "results");
        assertNull(base.defer());
        ToolDefinition deferred = base.defer(ToolDefer.AUTO);
        assertEquals(ToolDefer.AUTO, deferred.defer());
    }

    @Test
    void defer_setsNeverMode() {
        ToolDefinition base = ToolDefinition.from("must_preload", "Always preloaded", () -> "ok");
        ToolDefinition neverDeferred = base.defer(ToolDefer.NEVER);
        assertEquals(ToolDefer.NEVER, neverDeferred.defer());
    }

    @Test
    void defer_doesNotMutateOriginal() {
        ToolDefinition base = ToolDefinition.from("search", "Searches things", () -> "results");
        base.defer(ToolDefer.AUTO);
        assertNull(base.defer(), "original must remain unchanged");
    }

    @Test
    void fluentModifiers_canBeChained() {
        ToolDefinition tool = ToolDefinition.from("override_tool", "Overrides built-in", () -> "ok")
                .overridesBuiltInTool(true).skipPermission(true).defer(ToolDefer.AUTO);
        assertEquals(Boolean.TRUE, tool.overridesBuiltInTool());
        assertEquals(Boolean.TRUE, tool.skipPermission());
        assertEquals(ToolDefer.AUTO, tool.defer());
    }

    @Test
    void fluentModifiers_preserveHandlerAndSchema() throws Exception {
        Param<String> p = Param.of(String.class, "msg", "A message");
        ToolDefinition tool = ToolDefinition.from("echo", "Echoes message", p, msg -> msg).skipPermission(true)
                .overridesBuiltInTool(false);
        assertNotNull(tool.handler());
        Object result = tool.handler().invoke(invocationOf(Map.of("msg", "hello"))).get();
        assertEquals("hello", result);
    }

    // ── Group 6: Required/default semantics ──────────────────────────────────────

    @Test
    void requiredParam_passedValue_usesProvidedValue() throws Exception {
        Param<String> p = Param.of(String.class, "word", "A word");
        ToolDefinition tool = ToolDefinition.from("echo", "Echoes", p, w -> w);
        Object result = tool.handler().invoke(invocationOf(Map.of("word", "hello"))).get();
        assertEquals("hello", result);
    }

    @Test
    void requiredParam_missingFromInvocation_throwsIllegalArgumentException() {
        Param<String> p = Param.of(String.class, "word", "A required word");
        ToolDefinition tool = ToolDefinition.from("echo", "Echoes", p, w -> w);
        var ex = assertThrows(IllegalArgumentException.class, () -> tool.handler().invoke(invocationOf(Map.of())));
        assertTrue(ex.getMessage().contains("word"), "Exception message should mention the missing parameter name");
    }

    @Test
    void optionalParamWithDefault_absent_usesDefault() throws Exception {
        Param<Integer> p = Param.of(Integer.class, "limit", "Max results", false, "10");
        ToolDefinition tool = ToolDefinition.from("list", "Lists items", p, lim -> "limit=" + lim);
        Object result = tool.handler().invoke(invocationOf(Map.of())).get();
        assertEquals("limit=10", result);
    }

    @Test
    void optionalParamWithDefault_provided_usesProvidedValue() throws Exception {
        Param<Integer> p = Param.of(Integer.class, "limit", "Max results", false, "10");
        ToolDefinition tool = ToolDefinition.from("list", "Lists items", p, lim -> "limit=" + lim);
        Object result = tool.handler().invoke(invocationOf(Map.of("limit", 25))).get();
        assertEquals("limit=25", result);
    }

    @Test
    void optionalParamWithDefault_schemaNotInRequired() {
        Param<Integer> p = Param.of(Integer.class, "limit", "Max results", false, "10");
        ToolDefinition tool = ToolDefinition.from("list", "Lists items", p, lim -> "limit=" + lim);
        assertFalse(requiredOf(tool).contains("limit"));
        assertTrue(propertiesOf(tool).containsKey("limit"));
    }

    @Test
    void optionalParam_absent_noDefaultYieldsNull() throws Exception {
        Param<String> p = Param.of(String.class, "title", "Optional title", false, "");
        ToolDefinition tool = ToolDefinition.from("greet", "Greets", p, t -> t == null ? "(no title)" : t);
        Object result = tool.handler().invoke(invocationOf(Map.of())).get();
        assertEquals("(no title)", result);
    }

    @Test
    void defaultValueAppearsInSchema() {
        Param<Integer> p = Param.of(Integer.class, "limit", "Max results", false, "5");
        ToolDefinition tool = ToolDefinition.from("list", "Lists items", p, lim -> lim.toString());
        @SuppressWarnings("unchecked")
        Map<String, Object> limitPropSchema = (Map<String, Object>) propertiesOf(tool).get("limit");
        assertNotNull(limitPropSchema, "Schema must include 'limit' property");
        assertEquals(5, limitPropSchema.get("default"), "Default value must appear in schema");
    }

    // ── Group 7: Error / validation paths ────────────────────────────────────────

    @Test
    void from_nullName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ToolDefinition.from(null, "desc", () -> "ok"));
    }

    @Test
    void from_blankName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ToolDefinition.from("  ", "desc", () -> "ok"));
    }

    @Test
    void from_nullDescription_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ToolDefinition.from("tool", null, () -> "ok"));
    }

    @Test
    void from_blankDescription_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ToolDefinition.from("tool", "", () -> "ok"));
    }

    @Test
    void from_nullHandler_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> ToolDefinition.from("tool", "desc", (java.util.function.Supplier<String>) null));
    }

    @Test
    void from_oneArg_nullParam_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> ToolDefinition.from("tool", "desc", (Param<String>) null, s -> s));
    }

    @Test
    void from_twoArg_nullFirstParam_throwsIllegalArgumentException() {
        Param<String> p2 = Param.of(String.class, "b", "B param");
        assertThrows(IllegalArgumentException.class, () -> ToolDefinition.from("tool", "desc", null, p2, (a, b) -> a));
    }

    @Test
    void from_twoArg_nullSecondParam_throwsIllegalArgumentException() {
        Param<String> p1 = Param.of(String.class, "a", "A param");
        assertThrows(IllegalArgumentException.class, () -> ToolDefinition.from("tool", "desc", p1, null, (a, b) -> a));
    }

    @Test
    void from_twoArg_duplicateParamNames_throwsIllegalArgumentException() {
        Param<String> p1 = Param.of(String.class, "name", "Name 1");
        Param<String> p2 = Param.of(String.class, "name", "Name 2");
        var ex = assertThrows(IllegalArgumentException.class,
                () -> ToolDefinition.from("tool", "desc", p1, p2, (a, b) -> a + b));
        assertTrue(ex.getMessage().contains("name"), "error must mention the duplicate param name");
        assertTrue(ex.getMessage().contains("tool"), "error must mention the tool name");
    }

    @Test
    void fromAsync_nullName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> ToolDefinition.fromAsync(null, "desc", () -> CompletableFuture.completedFuture("ok")));
    }

    @Test
    void fromAsync_nullHandler_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ToolDefinition.fromAsync("tool", "desc",
                (java.util.function.Supplier<CompletableFuture<String>>) null));
    }

    @Test
    void fromWithToolInvocation_nullName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> ToolDefinition.fromWithToolInvocation(null, "desc", inv -> "ok"));
    }

    @Test
    void fromAsyncWithToolInvocation_nullDescription_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ToolDefinition.fromAsyncWithToolInvocation("tool", null,
                inv -> CompletableFuture.completedFuture("ok")));
    }

    // ── Group 8: Schema structure
    // ─────────────────────────────────────────────────

    @Test
    void schema_zeroArg_hasTypeObjectAndEmptyMaps() {
        ToolDefinition tool = ToolDefinition.from("noop", "No-op", () -> "done");
        Map<String, Object> schema = schemaOf(tool);
        assertEquals("object", schema.get("type"));
        assertTrue(((Map<?, ?>) schema.get("properties")).isEmpty());
        assertTrue(((List<?>) schema.get("required")).isEmpty());
    }

    @Test
    void schema_oneArg_hasCorrectTypeForString() {
        Param<String> p = Param.of(String.class, "query", "Search query");
        ToolDefinition tool = ToolDefinition.from("search", "Searches", p, q -> q);
        @SuppressWarnings("unchecked")
        Map<String, Object> querySchema = (Map<String, Object>) propertiesOf(tool).get("query");
        assertNotNull(querySchema);
        assertEquals("string", querySchema.get("type"));
        assertEquals("Search query", querySchema.get("description"));
    }

    @Test
    void schema_oneArg_hasCorrectTypeForInteger() {
        Param<Integer> p = Param.of(Integer.class, "count", "Item count");
        ToolDefinition tool = ToolDefinition.from("count_items", "Counts items", p, c -> c.toString());
        @SuppressWarnings("unchecked")
        Map<String, Object> countSchema = (Map<String, Object>) propertiesOf(tool).get("count");
        assertNotNull(countSchema);
        assertEquals("integer", countSchema.get("type"));
    }

    @Test
    void schema_oneArg_hasCorrectTypeForBoolean() {
        Param<Boolean> p = Param.of(Boolean.class, "enabled", "Whether enabled");
        ToolDefinition tool = ToolDefinition.from("toggle", "Toggles", p, e -> e.toString());
        @SuppressWarnings("unchecked")
        Map<String, Object> enabledSchema = (Map<String, Object>) propertiesOf(tool).get("enabled");
        assertNotNull(enabledSchema);
        assertEquals("boolean", enabledSchema.get("type"));
    }

    @Test
    void schema_oneArg_enumTypeHasStringAndEnumValues() {
        Param<Color> p = Param.of(Color.class, "color", "A color");
        ToolDefinition tool = ToolDefinition.from("paint", "Paints with a color", p, c -> c.name());
        @SuppressWarnings("unchecked")
        Map<String, Object> colorSchema = (Map<String, Object>) propertiesOf(tool).get("color");
        assertNotNull(colorSchema);
        assertEquals("string", colorSchema.get("type"));
        @SuppressWarnings("unchecked")
        List<String> enumValues = (List<String>) colorSchema.get("enum");
        assertNotNull(enumValues);
        assertTrue(enumValues.contains("RED"));
        assertTrue(enumValues.contains("GREEN"));
        assertTrue(enumValues.contains("BLUE"));
    }

    // ── Group 9: Result formatting
    // ────────────────────────────────────────────────

    @Test
    void resultFormatting_stringReturnedAsIs() throws Exception {
        ToolDefinition tool = ToolDefinition.from("echo", "Echoes", () -> "plain text");
        Object result = tool.handler().invoke(invocationOf(Map.of())).get();
        assertEquals("plain text", result);
    }

    @Test
    void resultFormatting_nullMappedToSuccess() throws Exception {
        ToolDefinition tool = ToolDefinition.from("noop", "No-op", () -> null);
        Object result = tool.handler().invoke(invocationOf(Map.of())).get();
        assertEquals("Success", result);
    }

    @Test
    void resultFormatting_nonStringSerializedToJson() throws Exception {
        Param<String> p = Param.of(String.class, "key", "Key name");
        ToolDefinition tool = ToolDefinition.from("to_map", "Wraps in map", p, k -> Map.of("key", k, "value", 42));
        Object result = tool.handler().invoke(invocationOf(Map.of("key", "x"))).get();
        assertNotNull(result);
        assertTrue(result instanceof String, "Non-String should be JSON-serialized to String");
        String json = (String) result;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        assertTrue(node.isObject(), "Result should be a JSON object");
        assertEquals("x", node.get("key").asText(), "JSON must contain key field with value 'x'");
        assertEquals(42, node.get("value").asInt(), "JSON must contain value field with value 42");
    }

    @Test
    void resultFormatting_integerSerializedToJson() throws Exception {
        ToolDefinition tool = ToolDefinition.from("forty_two", "Returns 42", () -> 42);
        Object result = tool.handler().invoke(invocationOf(Map.of())).get();
        assertEquals("42", result);
    }

    // ── Group 10: Argument coercion
    // ───────────────────────────────────────────────

    @Test
    void coercion_stringArgPassedThrough() throws Exception {
        Param<String> p = Param.of(String.class, "msg", "A message");
        ToolDefinition tool = ToolDefinition.from("echo", "Echoes message", p, m -> m);
        Object result = tool.handler().invoke(invocationOf(Map.of("msg", "hello world"))).get();
        assertEquals("hello world", result);
    }

    @Test
    void coercion_integerArgFromJsonNumber() throws Exception {
        Param<Integer> p = Param.of(Integer.class, "n", "An integer");
        ToolDefinition tool = ToolDefinition.from("double_it", "Doubles n", p, n -> String.valueOf(n * 2));
        Object result = tool.handler().invoke(invocationOf(Map.of("n", 7))).get();
        assertEquals("14", result);
    }

    @Test
    void coercion_booleanArg() throws Exception {
        Param<Boolean> p = Param.of(Boolean.class, "flag", "A flag");
        ToolDefinition tool = ToolDefinition.from("flagged", "Reports flag", p, f -> f ? "yes" : "no");
        Object result = tool.handler().invoke(invocationOf(Map.of("flag", true))).get();
        assertEquals("yes", result);
    }

    @Test
    void coercion_enumArgFromString() throws Exception {
        Param<Color> p = Param.of(Color.class, "color", "A color");
        ToolDefinition tool = ToolDefinition.from("paint", "Paints", p, c -> c.name().toLowerCase());
        Object result = tool.handler().invoke(invocationOf(Map.of("color", "GREEN"))).get();
        assertEquals("green", result);
    }

    @Test
    void coercion_defaultIntegerParsedCorrectly() throws Exception {
        Param<Integer> p = Param.of(Integer.class, "limit", "Max count", false, "99");
        ToolDefinition tool = ToolDefinition.from("bounded", "Bounded list", p, lim -> "got=" + lim);
        // No argument provided — should use default 99
        Object result = tool.handler().invoke(invocationOf(Map.of())).get();
        assertEquals("got=99", result);
    }

    // ── Inner types for test helpers
    // ──────────────────────────────────────────────

    enum Color {
        RED, GREEN, BLUE
    }
}
