/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * What initiated a conversation compaction
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CompactionTrigger {
    /** The {@code threshold} variant. */
    THRESHOLD("threshold"),
    /** The {@code context_limit_retry} variant. */
    CONTEXT_LIMIT_RETRY("context_limit_retry"),
    /** The {@code manual} variant. */
    MANUAL("manual"),
    /** The {@code memory_pressure} variant. */
    MEMORY_PRESSURE("memory_pressure"),
    /** The {@code model_switch} variant. */
    MODEL_SWITCH("model_switch");

    private final String value;
    CompactionTrigger(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CompactionTrigger fromValue(String value) {
        for (CompactionTrigger v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CompactionTrigger value: " + value);
    }
}
