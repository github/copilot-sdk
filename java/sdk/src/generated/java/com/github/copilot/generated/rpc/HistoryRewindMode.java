/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Scope of a rewind operation.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum HistoryRewindMode {
    /** The {@code conversation} variant. */
    CONVERSATION("conversation"),
    /** The {@code conversation-and-files} variant. */
    CONVERSATION_AND_FILES("conversation-and-files");

    private final String value;
    HistoryRewindMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static HistoryRewindMode fromValue(String value) {
        for (HistoryRewindMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown HistoryRewindMode value: " + value);
    }
}
