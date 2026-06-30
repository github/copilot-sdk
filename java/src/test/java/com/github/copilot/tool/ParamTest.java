/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Param} runtime parameter metadata.
 */
public class ParamTest {

    // ------------------------------------------------------------------
    // Factory method: of(type, name, description)
    // ------------------------------------------------------------------

    @Test
    void ofCreatesRequiredParamWithNoDefault() {
        Param<String> p = Param.of(String.class, "query", "Search query");
        assertEquals(String.class, p.type());
        assertEquals("query", p.name());
        assertEquals("Search query", p.description());
        assertTrue(p.required());
        assertEquals("", p.defaultValue());
        assertFalse(p.hasDefaultValue());
    }

    @Test
    void ofFullFactoryCreatesOptionalParamWithDefault() {
        Param<Integer> p = Param.of(Integer.class, "limit", "Max results", false, "10");
        assertEquals(Integer.class, p.type());
        assertEquals("limit", p.name());
        assertEquals("Max results", p.description());
        assertFalse(p.required());
        assertEquals("10", p.defaultValue());
        assertTrue(p.hasDefaultValue());
    }

    // ------------------------------------------------------------------
    // Validation: blank name/description rejected
    // ------------------------------------------------------------------

    @Test
    void rejectsNullName() {
        var ex = assertThrows(IllegalArgumentException.class, () -> Param.of(String.class, null, "desc"));
        assertTrue(ex.getMessage().contains("name"));
    }

    @Test
    void rejectsBlankName() {
        var ex = assertThrows(IllegalArgumentException.class, () -> Param.of(String.class, "  ", "desc"));
        assertTrue(ex.getMessage().contains("name"));
    }

    @Test
    void rejectsNullDescription() {
        var ex = assertThrows(IllegalArgumentException.class, () -> Param.of(String.class, "n", null));
        assertTrue(ex.getMessage().contains("description"));
    }

    @Test
    void rejectsBlankDescription() {
        var ex = assertThrows(IllegalArgumentException.class, () -> Param.of(String.class, "n", ""));
        assertTrue(ex.getMessage().contains("description"));
    }

    // ------------------------------------------------------------------
    // Validation: required=true with non-empty default rejected
    // ------------------------------------------------------------------

    @Test
    void rejectsRequiredWithNonEmptyDefault() {
        var ex = assertThrows(IllegalArgumentException.class, () -> Param.of(String.class, "x", "desc", true, "val"));
        assertTrue(ex.getMessage().contains("required=true"));
    }

    @Test
    void allowsRequiredWithEmptyDefault() {
        Param<String> p = Param.of(String.class, "x", "desc", true, "");
        assertTrue(p.required());
        assertFalse(p.hasDefaultValue());
    }

    @Test
    void allowsRequiredWithNullDefault() {
        Param<String> p = Param.of(String.class, "x", "desc", true, null);
        assertTrue(p.required());
        assertEquals("", p.defaultValue());
    }

    // ------------------------------------------------------------------
    // Validation: default value type checking
    // ------------------------------------------------------------------

    @Test
    void validatesIntegerDefault() {
        // valid
        Param<Integer> p = Param.of(Integer.class, "n", "num", false, "42");
        assertEquals("42", p.defaultValue());

        // invalid
        assertThrows(IllegalArgumentException.class, () -> Param.of(Integer.class, "n", "num", false, "abc"));
    }

    @Test
    void validatesLongDefault() {
        Param<Long> p = Param.of(Long.class, "n", "num", false, "999999999999");
        assertEquals("999999999999", p.defaultValue());

        assertThrows(IllegalArgumentException.class, () -> Param.of(Long.class, "n", "num", false, "notlong"));
    }

    @Test
    void validatesDoubleDefault() {
        Param<Double> p = Param.of(Double.class, "d", "decimal", false, "3.14");
        assertEquals("3.14", p.defaultValue());

        assertThrows(IllegalArgumentException.class, () -> Param.of(Double.class, "d", "decimal", false, "xyz"));
    }

    @Test
    void validatesFloatDefault() {
        Param<Float> p = Param.of(Float.class, "f", "float val", false, "1.5");
        assertEquals("1.5", p.defaultValue());

        assertThrows(IllegalArgumentException.class, () -> Param.of(Float.class, "f", "float val", false, "notfloat"));
    }

    @Test
    void validatesShortDefault() {
        Param<Short> p = Param.of(Short.class, "s", "short val", false, "100");
        assertEquals("100", p.defaultValue());

        assertThrows(IllegalArgumentException.class, () -> Param.of(Short.class, "s", "short val", false, "99999"));
    }

    @Test
    void validatesByteDefault() {
        Param<Byte> p = Param.of(Byte.class, "b", "byte val", false, "127");
        assertEquals("127", p.defaultValue());

        assertThrows(IllegalArgumentException.class, () -> Param.of(Byte.class, "b", "byte val", false, "999"));
    }

    @Test
    void validatesBooleanDefault() {
        Param<Boolean> p1 = Param.of(Boolean.class, "b", "flag", false, "true");
        assertEquals("true", p1.defaultValue());

        Param<Boolean> p2 = Param.of(Boolean.class, "b", "flag", false, "FALSE");
        assertEquals("FALSE", p2.defaultValue());

        assertThrows(IllegalArgumentException.class, () -> Param.of(Boolean.class, "b", "flag", false, "yes"));
    }

    @Test
    void validatesEnumDefault() {
        Param<TestEnum> p = Param.of(TestEnum.class, "e", "enum val", false, "ALPHA");
        assertEquals("ALPHA", p.defaultValue());

        assertThrows(IllegalArgumentException.class, () -> Param.of(TestEnum.class, "e", "enum val", false, "INVALID"));
    }

    @Test
    void rejectsUnsupportedTypeWithDefault() {
        assertThrows(IllegalArgumentException.class, () -> Param.of(Object.class, "o", "object", false, "something"));
    }

    @Test
    void allowsStringDefault() {
        Param<String> p = Param.of(String.class, "s", "string", false, "hello");
        assertEquals("hello", p.defaultValue());
    }

    // ------------------------------------------------------------------
    // Fluent mutators return new instances
    // ------------------------------------------------------------------

    @Test
    void nameMutatorReturnsNewInstance() {
        Param<String> original = Param.of(String.class, "a", "desc");
        Param<String> renamed = original.name("b");
        assertEquals("a", original.name());
        assertEquals("b", renamed.name());
    }

    @Test
    void descriptionMutatorReturnsNewInstance() {
        Param<String> original = Param.of(String.class, "a", "desc1");
        Param<String> updated = original.description("desc2");
        assertEquals("desc1", original.description());
        assertEquals("desc2", updated.description());
    }

    @Test
    void requiredMutatorReturnsNewInstance() {
        Param<String> original = Param.of(String.class, "a", "desc");
        Param<String> optional = original.required(false);
        assertTrue(original.required());
        assertFalse(optional.required());
    }

    @Test
    void defaultValueMutatorSetsOptional() {
        Param<String> original = Param.of(String.class, "a", "desc");
        Param<String> withDefault = original.defaultValue("val");
        assertTrue(original.required());
        assertFalse(withDefault.required());
        assertEquals("val", withDefault.defaultValue());
        assertTrue(withDefault.hasDefaultValue());
    }

    // ------------------------------------------------------------------
    // equals / hashCode / toString
    // ------------------------------------------------------------------

    @Test
    void equalParamsAreEqual() {
        Param<String> a = Param.of(String.class, "x", "desc");
        Param<String> b = Param.of(String.class, "x", "desc");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentParamsAreNotEqual() {
        Param<String> a = Param.of(String.class, "x", "desc");
        Param<String> b = Param.of(String.class, "y", "desc");
        assertNotEquals(a, b);
    }

    @Test
    void toStringContainsName() {
        Param<String> p = Param.of(String.class, "query", "Search");
        assertTrue(p.toString().contains("query"));
        assertTrue(p.toString().contains("String"));
    }

    // ------------------------------------------------------------------
    // Null type rejected
    // ------------------------------------------------------------------

    @Test
    void rejectsNullType() {
        assertThrows(NullPointerException.class, () -> Param.of(null, "n", "desc"));
    }

    // ------------------------------------------------------------------
    // Test enum for validation tests
    // ------------------------------------------------------------------

    enum TestEnum {
        ALPHA, BETA
    }
}
