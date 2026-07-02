/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.tool.Param;

/**
 * Internal runtime helper: maps {@link Param} metadata to JSON Schema
 * {@code Map} objects.
 *
 * <p>
 * This class is a simplified runtime counterpart to the compile-time
 * {@code SchemaGenerator}. It operates on {@code java.lang.reflect.Class}
 * values instead of {@code javax.lang.model} mirrors, and produces {@link Map}
 * instances rather than Java source-code literals. Unlike
 * {@code SchemaGenerator}, it does not inspect generics or object members
 * (records/POJOs) and therefore produces flat type mappings only (no
 * {@code additionalProperties} or nested object {@code properties}). It does
 * produce {@code items} for plain Java arrays via component-type recursion.
 *
 * <p>
 * Package-private: not part of the public API.
 */
class ParamSchema {

    /** Utility class; do not instantiate. */
    private ParamSchema() {
    }

    /**
     * Builds a JSON Schema {@code Map} from zero or more {@link Param} descriptors.
     *
     * <p>
     * Validation applied:
     * <ul>
     * <li>Each {@link Param} must be non-null.</li>
     * <li>Parameter names must be unique; duplicates throw
     * {@link IllegalArgumentException} with the tool name and duplicate name.</li>
     * </ul>
     *
     * @param toolName
     *            the tool name, included in exception messages for clarity
     * @param mapper
     *            the configured {@link ObjectMapper} used to coerce default values
     *            into their typed form for the schema
     * @param params
     *            zero or more parameter descriptors
     * @return a JSON Schema object map with {@code type=object},
     *         {@code properties}, and {@code required} keys
     * @throws IllegalArgumentException
     *             if a null param or duplicate parameter names are found
     */
    static Map<String, Object> buildSchema(String toolName, ObjectMapper mapper, Param<?>... params) {
        if (params == null || params.length == 0) {
            return Map.of("type", "object", "properties", Map.of(), "required", List.of());
        }

        // Validate: no null params, no duplicate names
        Set<String> seen = new HashSet<>();
        for (Param<?> param : params) {
            if (param == null) {
                throw new IllegalArgumentException("A Param descriptor is null for tool '" + toolName + "'");
            }
            if (!seen.add(param.name())) {
                throw new IllegalArgumentException(
                        "Duplicate parameter name '" + param.name() + "' in tool '" + toolName + "'");
            }
        }

        List<String> requiredNames = new ArrayList<>();
        Map<String, Object> properties = new LinkedHashMap<>();

        for (Param<?> param : params) {
            Map<String, Object> typeSchema = forType(param.type());
            Map<String, Object> enriched = new LinkedHashMap<>(typeSchema);
            enriched.put("description", param.description());
            if (param.hasDefaultValue()) {
                enriched.put("default", ParamCoercion.coerceDefault(param, mapper));
            }
            properties.put(param.name(), Collections.unmodifiableMap(enriched));
            if (param.required()) {
                requiredNames.add(param.name());
            }
        }

        return Map.of("type", "object", "properties", Collections.unmodifiableMap(properties), "required",
                Collections.unmodifiableList(requiredNames));
    }

    /**
     * Maps a Java {@link Class} to a flat JSON Schema type descriptor.
     *
     * <p>
     * Covers primitives, boxed types, strings, UUIDs, date-time types, enums,
     * collections, arrays, and maps. Does not resolve generic type parameters (e.g.
     * {@code List<T>} item schemas or {@code Map<K,V>} additionalProperties) —
     * those require the compile-time {@code SchemaGenerator} which operates on
     * {@code TypeMirror}.
     *
     * @param type
     *            the Java type to map
     * @return a JSON Schema type map (e.g. {@code Map.of("type", "string")})
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static Map<String, Object> forType(Class<?> type) {
        // Integer types
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class || type == byte.class
                || type == Byte.class || type == short.class || type == Short.class) {
            return Map.of("type", "integer");
        }
        // Floating-point types
        if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            return Map.of("type", "number");
        }
        // Boolean
        if (type == boolean.class || type == Boolean.class) {
            return Map.of("type", "boolean");
        }
        // Char → string
        if (type == char.class || type == Character.class) {
            return Map.of("type", "string");
        }
        // String
        if (type == String.class) {
            return Map.of("type", "string");
        }
        // UUID
        if (type == java.util.UUID.class) {
            return Map.of("type", "string", "format", "uuid");
        }
        // Optional primitive types
        if (type == java.util.OptionalInt.class || type == java.util.OptionalLong.class) {
            return Map.of("type", "integer");
        }
        if (type == java.util.OptionalDouble.class) {
            return Map.of("type", "number");
        }
        // Date-time types
        if (type == java.time.OffsetDateTime.class || type == java.time.LocalDateTime.class
                || type == java.time.Instant.class || type == java.time.ZonedDateTime.class) {
            return Map.of("type", "string", "format", "date-time");
        }
        if (type == java.time.LocalDate.class) {
            return Map.of("type", "string", "format", "date");
        }
        if (type == java.time.LocalTime.class) {
            return Map.of("type", "string", "format", "time");
        }
        // JsonNode / Object → any (no type constraint)
        if (type == com.fasterxml.jackson.databind.JsonNode.class || type == Object.class) {
            return Map.of();
        }
        // Enum types
        if (type.isEnum()) {
            Class<? extends Enum> enumType = (Class<? extends Enum>) type;
            List<String> constants = Arrays.stream(enumType.getEnumConstants()).map(Enum::name)
                    .collect(Collectors.toList());
            return Map.of("type", "string", "enum", Collections.unmodifiableList(constants));
        }
        // List / Collection / Set → array (raw element type)
        if (java.util.List.class.isAssignableFrom(type) || java.util.Collection.class.isAssignableFrom(type)
                || java.util.Set.class.isAssignableFrom(type)) {
            return Map.of("type", "array");
        }
        // Plain array → array with items schema derived from component type
        if (type.isArray()) {
            Map<String, Object> itemsSchema = forType(type.getComponentType());
            return Map.of("type", "array", "items", itemsSchema);
        }
        // Map → object
        if (java.util.Map.class.isAssignableFrom(type)) {
            return Map.of("type", "object");
        }
        // POJO / record → object
        return Map.of("type", "object");
    }
}
