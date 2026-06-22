/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Controls whether a {@link ToolDefinition} may be deferred (loaded lazily via
 * tool search) rather than always pre-loaded.
 * <p>
 * Set on
 * {@link ToolDefinition#createWithDefer(String, String, java.util.Map, ToolHandler, ToolDefer)}
 * to express the tool's deferral preference; defaults to letting the runtime
 * decide when unset.
 *
 * @see ToolDefinition
 * @since 1.0.0
 */
public enum ToolDefer {

    /**
     * No deferral preference set. Used as the default for annotation-based
     * tool definitions where the user has not explicitly chosen a mode.
     */
    NONE(""),

    /** The tool can be deferred and surfaced through tool search. */
    AUTO("auto"),

    /** The tool is always pre-loaded. */
    NEVER("never");

    private final String value;

    ToolDefer(String value) {
        this.value = value;
    }

    /**
     * Returns the JSON value for this deferral mode.
     *
     * @return the string value used in JSON serialization
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Deserializes a JSON string value into the corresponding {@code ToolDefer}
     * enum constant.
     *
     * @param value
     *            the JSON string value
     * @return the matching {@code ToolDefer}, or {@code null} if value is
     *         {@code null}
     * @throws IllegalArgumentException
     *             if the value does not match any known deferral mode
     */
    @JsonCreator
    public static ToolDefer fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ToolDefer mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown ToolDefer value: " + value);
    }
}
