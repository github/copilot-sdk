/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.tool.Param;

/**
 * Unit tests for {@link ParamCoercion} — runtime argument coercion from raw
 * invocation maps to typed Java values declared by {@link Param} descriptors.
 */
class ParamCoercionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── coerce: present argument, simple types ───────────────────────────────────

    @Test
    void coerce_stringArg_passedThrough() {
        Param<String> p = Param.of(String.class, "msg", "A message");
        String result = ParamCoercion.coerce(Map.of("msg", "hello"), p, MAPPER);
        assertEquals("hello", result);
    }

    @Test
    void coerce_integerArgFromNumber() {
        Param<Integer> p = Param.of(Integer.class, "n", "A number");
        Integer result = ParamCoercion.coerce(Map.of("n", 42), p, MAPPER);
        assertEquals(42, result);
    }

    @Test
    void coerce_longArgFromNumber() {
        Param<Long> p = Param.of(Long.class, "id", "An identifier");
        Long result = ParamCoercion.coerce(Map.of("id", 123456789L), p, MAPPER);
        assertEquals(123456789L, result);
    }

    @Test
    void coerce_doubleArgFromNumber() {
        Param<Double> p = Param.of(Double.class, "price", "A price");
        Double result = ParamCoercion.coerce(Map.of("price", 19.99), p, MAPPER);
        assertEquals(19.99, result, 0.001);
    }

    @Test
    void coerce_floatArgFromNumber() {
        Param<Float> p = Param.of(Float.class, "rate", "A rate");
        Float result = ParamCoercion.coerce(Map.of("rate", 3.14), p, MAPPER);
        assertEquals(3.14f, result, 0.01f);
    }

    @Test
    void coerce_booleanArgFromBoolean() {
        Param<Boolean> p = Param.of(Boolean.class, "flag", "A flag");
        Boolean result = ParamCoercion.coerce(Map.of("flag", true), p, MAPPER);
        assertEquals(true, result);
    }

    // Note: enum coercion via mapper.convertValue requires the enum's package to be
    // opened to com.fasterxml.jackson.databind. In the SDK module,
    // com.github.copilot.tool
    // is not opened to Jackson (only com.github.copilot.rpc is). User-defined enums
    // will
    // be outside the SDK module and fully accessible. Enum default coercion is
    // tested via
    // coerceDefault_enum which uses Enum.valueOf directly.

    @Test
    void coerce_enumFromString_viaCoerceDefault() {
        Param<TestMode> p = Param.of(TestMode.class, "mode", "Mode", false, "FAST");
        TestMode result = ParamCoercion.coerce(Map.of(), p, MAPPER);
        assertEquals(TestMode.FAST, result);
    }

    // ── coerce: Optional primitive types ─────────────────────────────────────────

    @Test
    void coerce_optionalInt_fromNumber() {
        Param<OptionalInt> p = Param.of(OptionalInt.class, "count", "Count", false, "");
        OptionalInt result = ParamCoercion.coerce(Map.of("count", 7), p, MAPPER);
        assertEquals(OptionalInt.of(7), result);
    }

    @Test
    void coerce_optionalLong_fromNumber() {
        Param<OptionalLong> p = Param.of(OptionalLong.class, "ts", "Timestamp", false, "");
        OptionalLong result = ParamCoercion.coerce(Map.of("ts", 999L), p, MAPPER);
        assertEquals(OptionalLong.of(999L), result);
    }

    @Test
    void coerce_optionalDouble_fromNumber() {
        Param<OptionalDouble> p = Param.of(OptionalDouble.class, "ratio", "Ratio", false, "");
        OptionalDouble result = ParamCoercion.coerce(Map.of("ratio", 2.5), p, MAPPER);
        assertEquals(OptionalDouble.of(2.5), result);
    }

    @Test
    void coerce_optionalInt_nonNumeric_throwsIllegalArgument() {
        Param<OptionalInt> p = Param.of(OptionalInt.class, "count", "Count", false, "");
        assertThrows(IllegalArgumentException.class,
                () -> ParamCoercion.coerce(Map.of("count", "not_a_number"), p, MAPPER));
    }

    @Test
    void coerce_optionalLong_nonNumeric_throwsIllegalArgument() {
        Param<OptionalLong> p = Param.of(OptionalLong.class, "ts", "Timestamp", false, "");
        assertThrows(IllegalArgumentException.class, () -> ParamCoercion.coerce(Map.of("ts", "abc"), p, MAPPER));
    }

    @Test
    void coerce_optionalDouble_nonNumeric_throwsIllegalArgument() {
        Param<OptionalDouble> p = Param.of(OptionalDouble.class, "ratio", "Ratio", false, "");
        assertThrows(IllegalArgumentException.class, () -> ParamCoercion.coerce(Map.of("ratio", "xyz"), p, MAPPER));
    }

    // ── coerce: missing argument — required ──────────────────────────────────────

    @Test
    void coerce_requiredMissing_throwsWithParamName() {
        Param<String> p = Param.of(String.class, "query", "Search query");
        var ex = assertThrows(IllegalArgumentException.class, () -> ParamCoercion.coerce(Map.of(), p, MAPPER));
        assertTrue(ex.getMessage().contains("query"));
    }

    @Test
    void coerce_requiredMissing_nullArgs_throws() {
        Param<String> p = Param.of(String.class, "name", "A name");
        var ex = assertThrows(IllegalArgumentException.class, () -> ParamCoercion.coerce(null, p, MAPPER));
        assertTrue(ex.getMessage().contains("name"));
    }

    // ── coerce: missing argument — optional with default ─────────────────────────

    @Test
    void coerce_optionalWithStringDefault_usesDefault() {
        Param<String> p = Param.of(String.class, "mode", "Mode", false, "normal");
        String result = ParamCoercion.coerce(Map.of(), p, MAPPER);
        assertEquals("normal", result);
    }

    @Test
    void coerce_optionalWithIntegerDefault_usesDefault() {
        Param<Integer> p = Param.of(Integer.class, "limit", "Limit", false, "25");
        Integer result = ParamCoercion.coerce(Map.of(), p, MAPPER);
        assertEquals(25, result);
    }

    @Test
    void coerce_optionalWithLongDefault_usesDefault() {
        Param<Long> p = Param.of(Long.class, "offset", "Offset", false, "100");
        Long result = ParamCoercion.coerce(Map.of(), p, MAPPER);
        assertEquals(100L, result);
    }

    @Test
    void coerce_optionalWithDoubleDefault_usesDefault() {
        Param<Double> p = Param.of(Double.class, "threshold", "Threshold", false, "0.75");
        Double result = ParamCoercion.coerce(Map.of(), p, MAPPER);
        assertEquals(0.75, result, 0.001);
    }

    @Test
    void coerce_optionalWithFloatDefault_usesDefault() {
        Param<Float> p = Param.of(Float.class, "rate", "Rate", false, "1.5");
        Float result = ParamCoercion.coerce(Map.of(), p, MAPPER);
        assertEquals(1.5f, result, 0.01f);
    }

    @Test
    void coerce_optionalWithShortDefault_usesDefault() {
        Param<Short> p = Param.of(Short.class, "level", "Level", false, "3");
        Short result = ParamCoercion.coerce(Map.of(), p, MAPPER);
        assertEquals((short) 3, result);
    }

    @Test
    void coerce_optionalWithByteDefault_usesDefault() {
        Param<Byte> p = Param.of(Byte.class, "code", "Code", false, "7");
        Byte result = ParamCoercion.coerce(Map.of(), p, MAPPER);
        assertEquals((byte) 7, result);
    }

    @Test
    void coerce_optionalWithBooleanDefault_usesDefault() {
        Param<Boolean> p = Param.of(Boolean.class, "verbose", "Verbose", false, "true");
        Boolean result = ParamCoercion.coerce(Map.of(), p, MAPPER);
        assertEquals(true, result);
    }

    @Test
    void coerce_optionalWithEnumDefault_usesDefault() {
        Param<TestMode> p = Param.of(TestMode.class, "mode", "Mode", false, "SLOW");
        TestMode result = ParamCoercion.coerce(Map.of(), p, MAPPER);
        assertEquals(TestMode.SLOW, result);
    }

    // ── coerce: missing argument — optional without default ──────────────────────

    @Test
    void coerce_optionalNoDefault_returnsNull() {
        Param<String> p = Param.of(String.class, "title", "Title", false, "");
        String result = ParamCoercion.coerce(Map.of(), p, MAPPER);
        assertNull(result);
    }

    @Test
    void coerce_optionalNoDefault_optionalInt_returnsEmpty() {
        Param<OptionalInt> p = Param.of(OptionalInt.class, "n", "Number", false, "");
        OptionalInt result = ParamCoercion.coerce(Map.of(), p, MAPPER);
        assertEquals(OptionalInt.empty(), result);
    }

    @Test
    void coerce_optionalNoDefault_optionalLong_returnsEmpty() {
        Param<OptionalLong> p = Param.of(OptionalLong.class, "ts", "Timestamp", false, "");
        OptionalLong result = ParamCoercion.coerce(Map.of(), p, MAPPER);
        assertEquals(OptionalLong.empty(), result);
    }

    @Test
    void coerce_optionalNoDefault_optionalDouble_returnsEmpty() {
        Param<OptionalDouble> p = Param.of(OptionalDouble.class, "ratio", "Ratio", false, "");
        OptionalDouble result = ParamCoercion.coerce(Map.of(), p, MAPPER);
        assertEquals(OptionalDouble.empty(), result);
    }

    // ── coerce: type conversion via ObjectMapper ─────────────────────────────────

    @Test
    void coerce_integerFromStringViaMapper() {
        // ObjectMapper can convert "42" string to Integer
        Param<Integer> p = Param.of(Integer.class, "n", "A number");
        Integer result = ParamCoercion.coerce(Map.of("n", "42"), p, MAPPER);
        assertEquals(42, result);
    }

    @Test
    void coerce_booleanFromStringViaMapper() {
        Param<Boolean> p = Param.of(Boolean.class, "flag", "A flag");
        Boolean result = ParamCoercion.coerce(Map.of("flag", "true"), p, MAPPER);
        assertEquals(true, result);
    }

    @Test
    void coerce_incompatibleType_throwsWithParamName() {
        Param<Integer> p = Param.of(Integer.class, "count", "Count");
        var ex = assertThrows(IllegalArgumentException.class,
                () -> ParamCoercion.coerce(Map.of("count", "not_a_number"), p, MAPPER));
        assertTrue(ex.getMessage().contains("count"));
    }

    // ── coerceDefault: direct tests ──────────────────────────────────────────────

    @Test
    void coerceDefault_string() {
        Param<String> p = Param.of(String.class, "s", "A string", false, "hello");
        assertEquals("hello", ParamCoercion.coerceDefault(p, MAPPER));
    }

    @Test
    void coerceDefault_integer() {
        Param<Integer> p = Param.of(Integer.class, "n", "A num", false, "99");
        assertEquals(99, ParamCoercion.coerceDefault(p, MAPPER));
    }

    @Test
    void coerceDefault_long() {
        Param<Long> p = Param.of(Long.class, "id", "An id", false, "12345");
        assertEquals(12345L, ParamCoercion.coerceDefault(p, MAPPER));
    }

    @Test
    void coerceDefault_double() {
        Param<Double> p = Param.of(Double.class, "d", "A double", false, "3.14");
        assertEquals(3.14, ParamCoercion.coerceDefault(p, MAPPER), 0.001);
    }

    @Test
    void coerceDefault_float() {
        Param<Float> p = Param.of(Float.class, "f", "A float", false, "2.5");
        assertEquals(2.5f, ParamCoercion.coerceDefault(p, MAPPER), 0.01f);
    }

    @Test
    void coerceDefault_short() {
        Param<Short> p = Param.of(Short.class, "s", "A short", false, "10");
        assertEquals((short) 10, ParamCoercion.coerceDefault(p, MAPPER));
    }

    @Test
    void coerceDefault_byte() {
        Param<Byte> p = Param.of(Byte.class, "b", "A byte", false, "5");
        assertEquals((byte) 5, ParamCoercion.coerceDefault(p, MAPPER));
    }

    @Test
    void coerceDefault_booleanTrue() {
        Param<Boolean> p = Param.of(Boolean.class, "v", "Verbose", false, "true");
        assertEquals(true, ParamCoercion.coerceDefault(p, MAPPER));
    }

    @Test
    void coerceDefault_booleanFalse() {
        Param<Boolean> p = Param.of(Boolean.class, "v", "Verbose", false, "false");
        assertEquals(false, ParamCoercion.coerceDefault(p, MAPPER));
    }

    @Test
    void coerceDefault_enum() {
        Param<TestMode> p = Param.of(TestMode.class, "m", "Mode", false, "FAST");
        assertEquals(TestMode.FAST, ParamCoercion.coerceDefault(p, MAPPER));
    }

    // ── emptyOptionalOrNull: direct tests ────────────────────────────────────────

    @Test
    void emptyOptionalOrNull_optionalInt_returnsEmpty() {
        assertEquals(OptionalInt.empty(), ParamCoercion.emptyOptionalOrNull(OptionalInt.class));
    }

    @Test
    void emptyOptionalOrNull_optionalLong_returnsEmpty() {
        assertEquals(OptionalLong.empty(), ParamCoercion.emptyOptionalOrNull(OptionalLong.class));
    }

    @Test
    void emptyOptionalOrNull_optionalDouble_returnsEmpty() {
        assertEquals(OptionalDouble.empty(), ParamCoercion.emptyOptionalOrNull(OptionalDouble.class));
    }

    @Test
    void emptyOptionalOrNull_string_returnsNull() {
        assertNull(ParamCoercion.emptyOptionalOrNull(String.class));
    }

    @Test
    void emptyOptionalOrNull_integer_returnsNull() {
        assertNull(ParamCoercion.emptyOptionalOrNull(Integer.class));
    }

    // ── Test helper types ────────────────────────────────────────────────────────

    enum TestMode {
        FAST, SLOW, NORMAL
    }
}
