/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Runtime reason the completion decision was accepted.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CompletionReceiptStopReason {
    /** The {@code natural} variant. */
    NATURAL("natural"),
    /** The {@code terminal_tool} variant. */
    TERMINAL_TOOL("terminal_tool"),
    /** The {@code agent_stop_block_limit} variant. */
    AGENT_STOP_BLOCK_LIMIT("agent_stop_block_limit");

    private final String value;
    CompletionReceiptStopReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CompletionReceiptStopReason fromValue(String value) {
        for (CompletionReceiptStopReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CompletionReceiptStopReason value: " + value);
    }
}
