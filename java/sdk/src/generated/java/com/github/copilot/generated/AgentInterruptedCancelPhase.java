/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Where the interruption landed relative to the first streamed token.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AgentInterruptedCancelPhase {
    /** The {@code pre_first_token} variant. */
    PRE_FIRST_TOKEN("pre_first_token"),
    /** The {@code mid_stream} variant. */
    MID_STREAM("mid_stream");

    private final String value;
    AgentInterruptedCancelPhase(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AgentInterruptedCancelPhase fromValue(String value) {
        for (AgentInterruptedCancelPhase v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AgentInterruptedCancelPhase value: " + value);
    }
}
