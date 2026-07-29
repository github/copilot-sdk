/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Coarse request-difficulty bucket for UX explainability
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AutoModeResolvedReasoningBucket {
    /** The {@code low} variant. */
    LOW("low"),
    /** The {@code medium} variant. */
    MEDIUM("medium"),
    /** The {@code high} variant. */
    HIGH("high");

    private final String value;
    AutoModeResolvedReasoningBucket(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AutoModeResolvedReasoningBucket fromValue(String value) {
        for (AutoModeResolvedReasoningBucket v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AutoModeResolvedReasoningBucket value: " + value);
    }
}
