/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.tool.Param;

/**
 * Unit tests for {@link ParamSchema} — runtime JSON Schema generation from
 * {@link Param} descriptors.
 */
class ParamSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── buildSchema: empty / zero params ─────────────────────────────────────────

    @Test
    void buildSchema_nullParams_returnsEmptySchema() {
        Map<String, Object> schema = ParamSchema.buildSchema("tool", MAPPER, (Param<?>[]) null);
        assertEquals("object", schema.get("type"));
        assertTrue(((Map<?, ?>) schema.get("properties")).isEmpty());
        assertTrue(((List<?>) schema.get("required")).isEmpty());
    }

    @Test
    void buildSchema_emptyArray_returnsEmptySchema() {
        Map<String, Object> schema = ParamSchema.buildSchema("tool", MAPPER);
        assertEquals("object", schema.get("type"));
        assertTrue(((Map<?, ?>) schema.get("properties")).isEmpty());
        assertTrue(((List<?>) schema.get("required")).isEmpty());
    }

    // ── buildSchema: validation ──────────────────────────────────────────────────

    @Test
    void buildSchema_nullParamElement_throwsWithToolName() {
        Param<String> p1 = Param.of(String.class, "a", "First");
        var ex = assertThrows(IllegalArgumentException.class,
                () -> ParamSchema.buildSchema("my_tool", MAPPER, p1, null));
        assertTrue(ex.getMessage().contains("my_tool"));
    }

    @Test
    void buildSchema_duplicateNames_throwsWithToolNameAndParamName() {
        Param<String> p1 = Param.of(String.class, "name", "First name");
        Param<String> p2 = Param.of(String.class, "name", "Second name");
        var ex = assertThrows(IllegalArgumentException.class,
                () -> ParamSchema.buildSchema("greeting", MAPPER, p1, p2));
        assertTrue(ex.getMessage().contains("name"));
        assertTrue(ex.getMessage().contains("greeting"));
    }

    // ── buildSchema: required / optional semantics ───────────────────────────────

    @Test
    void buildSchema_requiredParam_appearsInRequiredList() {
        Param<String> p = Param.of(String.class, "query", "Search query");
        Map<String, Object> schema = ParamSchema.buildSchema("search", MAPPER, p);
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schema.get("required");
        assertTrue(required.contains("query"));
    }

    @Test
    void buildSchema_optionalParam_notInRequiredList() {
        Param<Integer> p = Param.of(Integer.class, "limit", "Max results", false, "10");
        Map<String, Object> schema = ParamSchema.buildSchema("list", MAPPER, p);
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schema.get("required");
        assertTrue(required.isEmpty());
    }

    @Test
    void buildSchema_mixedRequiredAndOptional_onlyRequiredInList() {
        Param<String> pReq = Param.of(String.class, "query", "Search query");
        Param<Integer> pOpt = Param.of(Integer.class, "limit", "Max", false, "20");
        Map<String, Object> schema = ParamSchema.buildSchema("search", MAPPER, pReq, pOpt);
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schema.get("required");
        assertEquals(1, required.size());
        assertEquals("query", required.get(0));
    }

    // ── buildSchema: description and default in property ─────────────────────────

    @Test
    void buildSchema_paramDescription_appearsInPropertySchema() {
        Param<String> p = Param.of(String.class, "msg", "A message to send");
        Map<String, Object> schema = ParamSchema.buildSchema("send", MAPPER, p);
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> msgSchema = (Map<String, Object>) props.get("msg");
        assertEquals("A message to send", msgSchema.get("description"));
    }

    @Test
    void buildSchema_paramDefault_appearsInPropertySchema() {
        Param<Integer> p = Param.of(Integer.class, "count", "Item count", false, "5");
        Map<String, Object> schema = ParamSchema.buildSchema("items", MAPPER, p);
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> countSchema = (Map<String, Object>) props.get("count");
        assertEquals(5, countSchema.get("default"));
    }

    @Test
    void buildSchema_stringDefault_appearsAsString() {
        Param<String> p = Param.of(String.class, "mode", "Operating mode", false, "fast");
        Map<String, Object> schema = ParamSchema.buildSchema("run", MAPPER, p);
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> modeSchema = (Map<String, Object>) props.get("mode");
        assertEquals("fast", modeSchema.get("default"));
    }

    @Test
    void buildSchema_booleanDefault_appearsAsBoolean() {
        Param<Boolean> p = Param.of(Boolean.class, "verbose", "Verbose mode", false, "true");
        Map<String, Object> schema = ParamSchema.buildSchema("run", MAPPER, p);
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> verboseSchema = (Map<String, Object>) props.get("verbose");
        assertEquals(true, verboseSchema.get("default"));
    }

    // ── buildSchema: multiple params preserve order ──────────────────────────────

    @Test
    void buildSchema_multipleParams_orderPreservedInProperties() {
        Param<String> p1 = Param.of(String.class, "alpha", "First");
        Param<String> p2 = Param.of(String.class, "beta", "Second");
        Param<String> p3 = Param.of(String.class, "gamma", "Third");
        Map<String, Object> schema = ParamSchema.buildSchema("ordered", MAPPER, p1, p2, p3);
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");
        List<String> keys = List.copyOf(props.keySet());
        assertEquals(List.of("alpha", "beta", "gamma"), keys);
    }

    // ── forType: primitive and boxed integer types ───────────────────────────────

    @Test
    void forType_int_returnsInteger() {
        assertEquals(Map.of("type", "integer"), ParamSchema.forType(int.class));
    }

    @Test
    void forType_Integer_returnsInteger() {
        assertEquals(Map.of("type", "integer"), ParamSchema.forType(Integer.class));
    }

    @Test
    void forType_long_returnsInteger() {
        assertEquals(Map.of("type", "integer"), ParamSchema.forType(long.class));
    }

    @Test
    void forType_Long_returnsInteger() {
        assertEquals(Map.of("type", "integer"), ParamSchema.forType(Long.class));
    }

    @Test
    void forType_short_returnsInteger() {
        assertEquals(Map.of("type", "integer"), ParamSchema.forType(short.class));
    }

    @Test
    void forType_Short_returnsInteger() {
        assertEquals(Map.of("type", "integer"), ParamSchema.forType(Short.class));
    }

    @Test
    void forType_byte_returnsInteger() {
        assertEquals(Map.of("type", "integer"), ParamSchema.forType(byte.class));
    }

    @Test
    void forType_Byte_returnsInteger() {
        assertEquals(Map.of("type", "integer"), ParamSchema.forType(Byte.class));
    }

    // ── forType: floating-point types ────────────────────────────────────────────

    @Test
    void forType_double_returnsNumber() {
        assertEquals(Map.of("type", "number"), ParamSchema.forType(double.class));
    }

    @Test
    void forType_Double_returnsNumber() {
        assertEquals(Map.of("type", "number"), ParamSchema.forType(Double.class));
    }

    @Test
    void forType_float_returnsNumber() {
        assertEquals(Map.of("type", "number"), ParamSchema.forType(float.class));
    }

    @Test
    void forType_Float_returnsNumber() {
        assertEquals(Map.of("type", "number"), ParamSchema.forType(Float.class));
    }

    // ── forType: boolean ─────────────────────────────────────────────────────────

    @Test
    void forType_boolean_returnsBoolean() {
        assertEquals(Map.of("type", "boolean"), ParamSchema.forType(boolean.class));
    }

    @Test
    void forType_Boolean_returnsBoolean() {
        assertEquals(Map.of("type", "boolean"), ParamSchema.forType(Boolean.class));
    }

    // ── forType: char / Character ────────────────────────────────────────────────

    @Test
    void forType_char_returnsString() {
        assertEquals(Map.of("type", "string"), ParamSchema.forType(char.class));
    }

    @Test
    void forType_Character_returnsString() {
        assertEquals(Map.of("type", "string"), ParamSchema.forType(Character.class));
    }

    // ── forType: String ──────────────────────────────────────────────────────────

    @Test
    void forType_String_returnsString() {
        assertEquals(Map.of("type", "string"), ParamSchema.forType(String.class));
    }

    // ── forType: UUID ────────────────────────────────────────────────────────────

    @Test
    void forType_UUID_returnsStringWithUuidFormat() {
        Map<String, Object> schema = ParamSchema.forType(UUID.class);
        assertEquals("string", schema.get("type"));
        assertEquals("uuid", schema.get("format"));
    }

    // ── forType: Optional primitive types ────────────────────────────────────────

    @Test
    void forType_OptionalInt_returnsInteger() {
        assertEquals(Map.of("type", "integer"), ParamSchema.forType(OptionalInt.class));
    }

    @Test
    void forType_OptionalLong_returnsInteger() {
        assertEquals(Map.of("type", "integer"), ParamSchema.forType(OptionalLong.class));
    }

    @Test
    void forType_OptionalDouble_returnsNumber() {
        assertEquals(Map.of("type", "number"), ParamSchema.forType(OptionalDouble.class));
    }

    // ── forType: date-time types ─────────────────────────────────────────────────

    @Test
    void forType_OffsetDateTime_returnsDateTimeFormat() {
        Map<String, Object> schema = ParamSchema.forType(OffsetDateTime.class);
        assertEquals("string", schema.get("type"));
        assertEquals("date-time", schema.get("format"));
    }

    @Test
    void forType_LocalDateTime_returnsDateTimeFormat() {
        Map<String, Object> schema = ParamSchema.forType(LocalDateTime.class);
        assertEquals("string", schema.get("type"));
        assertEquals("date-time", schema.get("format"));
    }

    @Test
    void forType_Instant_returnsDateTimeFormat() {
        Map<String, Object> schema = ParamSchema.forType(Instant.class);
        assertEquals("string", schema.get("type"));
        assertEquals("date-time", schema.get("format"));
    }

    @Test
    void forType_ZonedDateTime_returnsDateTimeFormat() {
        Map<String, Object> schema = ParamSchema.forType(ZonedDateTime.class);
        assertEquals("string", schema.get("type"));
        assertEquals("date-time", schema.get("format"));
    }

    @Test
    void forType_LocalDate_returnsDateFormat() {
        Map<String, Object> schema = ParamSchema.forType(LocalDate.class);
        assertEquals("string", schema.get("type"));
        assertEquals("date", schema.get("format"));
    }

    @Test
    void forType_LocalTime_returnsTimeFormat() {
        Map<String, Object> schema = ParamSchema.forType(LocalTime.class);
        assertEquals("string", schema.get("type"));
        assertEquals("time", schema.get("format"));
    }

    // ── forType: JsonNode / Object → any ─────────────────────────────────────────

    @Test
    void forType_JsonNode_returnsEmptySchema() {
        assertTrue(ParamSchema.forType(JsonNode.class).isEmpty());
    }

    @Test
    void forType_Object_returnsEmptySchema() {
        assertTrue(ParamSchema.forType(Object.class).isEmpty());
    }

    // ── forType: enums ───────────────────────────────────────────────────────────

    @Test
    void forType_enum_returnsStringWithEnumValues() {
        Map<String, Object> schema = ParamSchema.forType(TestColor.class);
        assertEquals("string", schema.get("type"));
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) schema.get("enum");
        assertNotNull(values);
        assertEquals(List.of("RED", "GREEN", "BLUE"), values);
    }

    // ── forType: collections ─────────────────────────────────────────────────────

    @Test
    void forType_List_returnsArray() {
        assertEquals(Map.of("type", "array"), ParamSchema.forType(List.class));
    }

    @Test
    void forType_Set_returnsArray() {
        assertEquals(Map.of("type", "array"), ParamSchema.forType(Set.class));
    }

    @Test
    void forType_Collection_returnsArray() {
        assertEquals(Map.of("type", "array"), ParamSchema.forType(Collection.class));
    }

    // ── forType: arrays ──────────────────────────────────────────────────────────

    @Test
    void forType_stringArray_returnsArrayWithStringItems() {
        Map<String, Object> schema = ParamSchema.forType(String[].class);
        assertEquals("array", schema.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> items = (Map<String, Object>) schema.get("items");
        assertEquals("string", items.get("type"));
    }

    @Test
    void forType_intArray_returnsArrayWithIntegerItems() {
        Map<String, Object> schema = ParamSchema.forType(int[].class);
        assertEquals("array", schema.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> items = (Map<String, Object>) schema.get("items");
        assertEquals("integer", items.get("type"));
    }

    @Test
    void forType_doubleArray_returnsArrayWithNumberItems() {
        Map<String, Object> schema = ParamSchema.forType(double[].class);
        assertEquals("array", schema.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> items = (Map<String, Object>) schema.get("items");
        assertEquals("number", items.get("type"));
    }

    // ── forType: Map ─────────────────────────────────────────────────────────────

    @Test
    void forType_Map_returnsObject() {
        assertEquals(Map.of("type", "object"), ParamSchema.forType(Map.class));
    }

    // ── forType: POJO / record fallback ──────────────────────────────────────────

    @Test
    void forType_record_returnsObject() {
        assertEquals(Map.of("type", "object"), ParamSchema.forType(TestRecord.class));
    }

    @Test
    void forType_pojo_returnsObject() {
        assertEquals(Map.of("type", "object"), ParamSchema.forType(TestPojo.class));
    }

    // ── Test helper types ────────────────────────────────────────────────────────

    enum TestColor {
        RED, GREEN, BLUE
    }

    record TestRecord(String name, int value) {
    }

    static class TestPojo {
        String field;
    }
}
