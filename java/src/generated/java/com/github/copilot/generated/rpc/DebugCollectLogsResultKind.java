/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Destination kind that was written.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum DebugCollectLogsResultKind {
    /** The {@code archive} variant. */
    ARCHIVE("archive"),
    /** The {@code directory} variant. */
    DIRECTORY("directory");

    private final String value;
    DebugCollectLogsResultKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static DebugCollectLogsResultKind fromValue(String value) {
        for (DebugCollectLogsResultKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown DebugCollectLogsResultKind value: " + value);
    }
}
