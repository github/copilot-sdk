/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Resolved Anthropic adaptive-thinking capability for a model.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AdaptiveThinkingSupport {
    /** The {@code unsupported} variant. */
    UNSUPPORTED("unsupported"),
    /** The {@code optional} variant. */
    OPTIONAL("optional"),
    /** The {@code required} variant. */
    REQUIRED("required");

    private final String value;
    AdaptiveThinkingSupport(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AdaptiveThinkingSupport fromValue(String value) {
        for (AdaptiveThinkingSupport v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AdaptiveThinkingSupport value: " + value);
    }
}
