/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.tool;

import java.util.Objects;

import com.github.copilot.CopilotExperimental;

/**
 * Runtime parameter metadata for lambda-defined tools.
 *
 * <p>
 * Each {@code Param} instance describes a single parameter that a tool accepts,
 * including its Java type, wire name, description, whether it is required, and
 * an optional default value. Instances are immutable; fluent mutators return
 * new copies.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * Param<String> query = Param.of(String.class, "query", "Search query text");
 *
 * Param<Integer> limit = Param.of(Integer.class, "limit", "Max results", false, "10");
 * }</pre>
 *
 * @param <T>
 *            the Java type of the parameter value
 * @since 1.0.6
 */
@CopilotExperimental
public final class Param<T> {

    private final Class<T> type;
    private final String name;
    private final String description;
    private final boolean required;
    private final String defaultValue;

    private Param(Class<T> type, String name, String description, boolean required, String defaultValue) {
        this.type = Objects.requireNonNull(type, "type");
        this.name = requireNonBlank(name, "name");
        this.description = requireNonBlank(description, "description");
        this.defaultValue = defaultValue == null ? "" : defaultValue;
        this.required = required;

        if (this.required && !this.defaultValue.isEmpty()) {
            throw new IllegalArgumentException("required=true cannot be combined with a non-empty defaultValue");
        }

        validateDefaultValue(type, this.defaultValue);
    }

    /**
     * Creates a required parameter with no default value.
     *
     * @param <T>
     *            the parameter type
     * @param type
     *            the Java class of the parameter
     * @param name
     *            the wire name sent to the model (must not be blank)
     * @param description
     *            a human-readable description (must not be blank)
     * @return a new {@code Param} instance
     * @throws NullPointerException
     *             if {@code type} is null
     * @throws IllegalArgumentException
     *             if {@code name} or {@code description} is blank
     */
    public static <T> Param<T> of(Class<T> type, String name, String description) {
        return new Param<>(type, name, description, true, "");
    }

    /**
     * Creates a parameter with explicit required/default settings.
     *
     * @param <T>
     *            the parameter type
     * @param type
     *            the Java class of the parameter
     * @param name
     *            the wire name sent to the model (must not be blank)
     * @param description
     *            a human-readable description (must not be blank)
     * @param required
     *            whether the parameter is required
     * @param defaultValue
     *            the default value as a string, or {@code null}/empty for none
     * @return a new {@code Param} instance
     * @throws NullPointerException
     *             if {@code type} is null
     * @throws IllegalArgumentException
     *             if validation fails
     */
    public static <T> Param<T> of(Class<T> type, String name, String description, boolean required,
            String defaultValue) {
        return new Param<>(type, name, description, required, defaultValue);
    }

    /**
     * Returns a copy with a different name.
     *
     * @param name
     *            the new parameter name
     * @return a new {@code Param} with the updated name
     */
    public Param<T> name(String name) {
        return new Param<>(this.type, name, this.description, this.required, this.defaultValue);
    }

    /**
     * Returns a copy with a different description.
     *
     * @param description
     *            the new description
     * @return a new {@code Param} with the updated description
     */
    public Param<T> description(String description) {
        return new Param<>(this.type, this.name, description, this.required, this.defaultValue);
    }

    /**
     * Returns a copy with a different required flag.
     *
     * @param required
     *            whether the parameter is required
     * @return a new {@code Param} with the updated required flag
     */
    public Param<T> required(boolean required) {
        return new Param<>(this.type, this.name, this.description, required, this.defaultValue);
    }

    /**
     * Returns an optional copy with the given default value. Setting a default
     * implicitly makes the parameter optional ({@code required=false}).
     *
     * @param defaultValue
     *            the default value as a string
     * @return a new {@code Param} with the default applied and required set to
     *         false
     */
    public Param<T> defaultValue(String defaultValue) {
        return new Param<>(this.type, this.name, this.description, false, defaultValue);
    }

    /** Returns the Java type of this parameter. */
    public Class<T> type() {
        return type;
    }

    /** Returns the wire name of this parameter. */
    public String name() {
        return name;
    }

    /** Returns the human-readable description. */
    public String description() {
        return description;
    }

    /** Returns whether this parameter is required. */
    public boolean required() {
        return required;
    }

    /** Returns the default value string, or empty if none. */
    public String defaultValue() {
        return defaultValue;
    }

    /** Returns {@code true} if a non-empty default value is set. */
    public boolean hasDefaultValue() {
        return !defaultValue.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Param<?> other)) {
            return false;
        }
        return required == other.required && Objects.equals(type, other.type) && Objects.equals(name, other.name)
                && Objects.equals(description, other.description) && Objects.equals(defaultValue, other.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, description, required, defaultValue);
    }

    @Override
    public String toString() {
        return "Param[name=" + name + ", type=" + type.getSimpleName() + ", required=" + required + "]";
    }

    // ------------------------------------------------------------------
    // Internal validation helpers
    // ------------------------------------------------------------------

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
        return value;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> void validateDefaultValue(Class<T> type, String defaultValue) {
        if (defaultValue == null || defaultValue.isEmpty()) {
            return;
        }

        try {
            if (type == String.class) {
                return;
            }
            if (type == Integer.class || type == int.class) {
                Integer.parseInt(defaultValue);
                return;
            }
            if (type == Long.class || type == long.class) {
                Long.parseLong(defaultValue);
                return;
            }
            if (type == Double.class || type == double.class) {
                Double.parseDouble(defaultValue);
                return;
            }
            if (type == Float.class || type == float.class) {
                Float.parseFloat(defaultValue);
                return;
            }
            if (type == Short.class || type == short.class) {
                Short.parseShort(defaultValue);
                return;
            }
            if (type == Byte.class || type == byte.class) {
                Byte.parseByte(defaultValue);
                return;
            }
            if (type == Boolean.class || type == boolean.class) {
                if (!"true".equalsIgnoreCase(defaultValue) && !"false".equalsIgnoreCase(defaultValue)) {
                    throw new IllegalArgumentException("must be 'true' or 'false'");
                }
                return;
            }
            if (type.isEnum()) {
                Class<? extends Enum> enumType = (Class<? extends Enum>) type;
                Enum.valueOf(enumType, defaultValue);
                return;
            }
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "defaultValue '" + defaultValue + "' is not valid for type " + type.getSimpleName(), ex);
        }

        throw new IllegalArgumentException(
                "defaultValue is not supported for type " + type.getName() + " without a custom coercion policy");
    }
}
