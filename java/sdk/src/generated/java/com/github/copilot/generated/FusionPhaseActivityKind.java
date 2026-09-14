/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Content-safe activity observed while a HydraFusion phase is running.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum FusionPhaseActivityKind {
    /** The {@code model_output} variant. */
    MODEL_OUTPUT("model_output"),
    /** The {@code tool_started} variant. */
    TOOL_STARTED("tool_started"),
    /** The {@code tool_completed} variant. */
    TOOL_COMPLETED("tool_completed");

    private final String value;
    FusionPhaseActivityKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FusionPhaseActivityKind fromValue(String value) {
        for (FusionPhaseActivityKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FusionPhaseActivityKind value: " + value);
    }
}
