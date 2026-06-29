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
     * No deferral preference set. This is an <b>annotation-only sentinel</b> used
     * as the default for {@code @CopilotTool(defer = ToolDefer.NONE)}.
     * <p>
     * This constant must <b>not</b> be passed to {@link ToolDefinition} factory
     * methods. The annotation processor and {@code ToolDefinition.fromObject()}
     * must map {@code NONE} to a {@code null} field reference so that
     * {@code @JsonInclude(NON_NULL)} on {@link ToolDefinition} omits the
     * {@code defer} key from the JSON-RPC wire payload entirely (matching the
     * nullable/optional semantics used by all other SDKs).
     * <p>
     * As a secondary safety net, {@link #getValue()} returns {@code null} for this
     * constant. Note that this alone does <b>not</b> cause field omission: if a
     * non-null {@code NONE} reference reaches a {@link ToolDefinition} field,
     * Jackson's {@code @JsonInclude(NON_NULL)} will still emit the field (as
     * {@code "defer": null}) because the field reference itself is not null. The
     * primary protection is mapping {@code NONE} to a null field reference before
     * constructing the {@link ToolDefinition}.
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
     * <p>
     * Returns {@code null} for {@link #NONE} to avoid emitting an empty string
     * ({@code "defer": ""}) if this sentinel accidentally reaches serialization.
     * With {@code null}, the worst-case leak becomes {@code "defer": null} rather
     * than an invalid empty string.
     *
     * @return the string value used in JSON serialization, or {@code null} for
     *         {@link #NONE}
     */
    @JsonValue
    public String getValue() {
        return this == NONE ? null : value;
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
