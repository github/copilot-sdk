/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * How a collected debug entry should be redacted before being staged.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum DebugCollectLogsRedaction {
    /** The {@code plain-text} variant. */
    PLAIN_TEXT("plain-text"),
    /** The {@code events-jsonl} variant. */
    EVENTS_JSONL("events-jsonl");

    private final String value;
    DebugCollectLogsRedaction(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static DebugCollectLogsRedaction fromValue(String value) {
        for (DebugCollectLogsRedaction v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown DebugCollectLogsRedaction value: " + value);
    }
}
