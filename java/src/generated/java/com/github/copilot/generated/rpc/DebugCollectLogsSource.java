/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Source category for a collected debug bundle entry.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum DebugCollectLogsSource {
    /** The {@code events} variant. */
    EVENTS("events"),
    /** The {@code process-log} variant. */
    PROCESS_LOG("process-log"),
    /** The {@code shell-log} variant. */
    SHELL_LOG("shell-log"),
    /** The {@code additional} variant. */
    ADDITIONAL("additional");

    private final String value;
    DebugCollectLogsSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static DebugCollectLogsSource fromValue(String value) {
        for (DebugCollectLogsSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown DebugCollectLogsSource value: " + value);
    }
}
