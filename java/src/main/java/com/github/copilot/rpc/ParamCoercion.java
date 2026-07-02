/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.tool.Param;

/**
 * Internal runtime helper: coerces raw invocation arguments to the typed values
 * declared by {@link Param} descriptors.
 *
 * <p>
 * Reuses the SDK-configured {@link ObjectMapper} for complex type conversions,
 * matching the coercion policy applied by existing ergonomic tooling. No
 * bespoke conversion paths are introduced.
 *
 * <p>
 * Package-private: not part of the public API.
 */
class ParamCoercion {

    /** Utility class; do not instantiate. */
    private ParamCoercion() {
    }

    /**
     * Coerces the named argument from an invocation argument map to the Java type
     * declared by {@code param}.
     *
     * <p>
     * Resolution order:
     * <ol>
     * <li>If the argument is present, convert it to {@code T} via
     * {@link ObjectMapper#convertValue}.</li>
     * <li>If absent and a default value is set, parse the string default via
     * {@link #coerceDefault}.</li>
     * <li>If absent and the parameter is optional ({@code required=false}), return
     * an empty Optional variant or {@code null}.</li>
     * <li>If absent and required, throw {@link IllegalArgumentException} with the
     * parameter name.</li>
     * </ol>
     *
     * @param <T>
     *            the target Java type
     * @param args
     *            the invocation argument map; may be {@code null} for zero-argument
     *            tools
     * @param param
     *            the parameter descriptor
     * @param mapper
     *            the configured {@link ObjectMapper} for complex type conversion
     * @return the coerced argument value
     * @throws IllegalArgumentException
     *             if a required parameter is missing or coercion fails
     */
    @SuppressWarnings("unchecked")
    static <T> T coerce(Map<String, Object> args, Param<T> param, ObjectMapper mapper) {
        Object raw = (args != null) ? args.get(param.name()) : null;

        if (raw == null) {
            if (param.hasDefaultValue()) {
                return coerceDefault(param, mapper);
            } else if (!param.required()) {
                return (T) emptyOptionalOrNull(param.type());
            } else {
                throw new IllegalArgumentException(
                        "Required parameter '" + param.name() + "' is missing from tool invocation");
            }
        }

        Class<T> type = param.type();

        // Handle Optional* types explicitly before delegating to ObjectMapper
        if (type == java.util.OptionalInt.class) {
            try {
                return (T) java.util.OptionalInt.of(((Number) raw).intValue());
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Parameter '" + param.name()
                        + "' expected a numeric value for OptionalInt, got: " + raw.getClass().getSimpleName(), ex);
            }
        }
        if (type == java.util.OptionalLong.class) {
            try {
                return (T) java.util.OptionalLong.of(((Number) raw).longValue());
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Parameter '" + param.name()
                        + "' expected a numeric value for OptionalLong, got: " + raw.getClass().getSimpleName(), ex);
            }
        }
        if (type == java.util.OptionalDouble.class) {
            try {
                return (T) java.util.OptionalDouble.of(((Number) raw).doubleValue());
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Parameter '" + param.name()
                        + "' expected a numeric value for OptionalDouble, got: " + raw.getClass().getSimpleName(), ex);
            }
        }

        try {
            return mapper.convertValue(raw, type);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Failed to coerce parameter '" + param.name() + "' to type " + type.getSimpleName(), ex);
        }
    }

    /**
     * Parses a {@link Param}'s string default value into the declared Java type.
     *
     * <p>
     * Handles primitives, boxed types, {@link String}, {@link Boolean}, and enums
     * explicitly, mirroring the validation logic in {@link Param}. The
     * {@link ObjectMapper#readValue} fallback exists as a safety net but is not
     * expected to be reached in practice, since {@link Param} construction rejects
     * defaults for non-primitive/boxed/String/Boolean/enum types.
     *
     * @param <T>
     *            the target Java type
     * @param param
     *            the parameter descriptor carrying the default value
     * @param mapper
     *            the configured {@link ObjectMapper} used as fallback for complex
     *            types
     * @return the parsed default value
     * @throws IllegalArgumentException
     *             if parsing fails
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static <T> T coerceDefault(Param<T> param, ObjectMapper mapper) {
        String defaultValue = param.defaultValue();
        Class<T> type = param.type();
        try {
            if (type == String.class) {
                return type.cast(defaultValue);
            }
            if (type == Integer.class || type == int.class) {
                return (T) Integer.valueOf(defaultValue);
            }
            if (type == Long.class || type == long.class) {
                return (T) Long.valueOf(defaultValue);
            }
            if (type == Double.class || type == double.class) {
                return (T) Double.valueOf(defaultValue);
            }
            if (type == Float.class || type == float.class) {
                return (T) Float.valueOf(defaultValue);
            }
            if (type == Short.class || type == short.class) {
                return (T) Short.valueOf(defaultValue);
            }
            if (type == Byte.class || type == byte.class) {
                return (T) Byte.valueOf(defaultValue);
            }
            if (type == Boolean.class || type == boolean.class) {
                return (T) Boolean.valueOf(defaultValue);
            }
            if (type.isEnum()) {
                Class<? extends Enum> enumType = (Class<? extends Enum>) type;
                return type.cast(Enum.valueOf(enumType, defaultValue));
            }
            // Fallback: let ObjectMapper parse the JSON-encoded default string
            return mapper.readValue(defaultValue, type);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to apply default value '" + defaultValue + "' for parameter '"
                    + param.name() + "' of type " + type.getSimpleName(), ex);
        }
    }

    /**
     * Returns an empty Optional variant for Optional primitive types, or
     * {@code null} for all other types.
     *
     * @param type
     *            the declared parameter type
     * @return {@link java.util.OptionalInt#empty()},
     *         {@link java.util.OptionalLong#empty()},
     *         {@link java.util.OptionalDouble#empty()}, or {@code null}
     */
    static Object emptyOptionalOrNull(Class<?> type) {
        if (type == java.util.OptionalInt.class) {
            return java.util.OptionalInt.empty();
        }
        if (type == java.util.OptionalLong.class) {
            return java.util.OptionalLong.empty();
        }
        if (type == java.util.OptionalDouble.class) {
            return java.util.OptionalDouble.empty();
        }
        return null;
    }
}
