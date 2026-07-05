/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Kind of caller-provided debug log entry.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum DebugCollectLogsEntryKind {
    /** The {@code file} variant. */
    FILE("file"),
    /** The {@code directory} variant. */
    DIRECTORY("directory");

    private final String value;
    DebugCollectLogsEntryKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static DebugCollectLogsEntryKind fromValue(String value) {
        for (DebugCollectLogsEntryKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown DebugCollectLogsEntryKind value: " + value);
    }
}
