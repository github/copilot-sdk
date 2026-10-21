/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Whether one atomic settings edit changed a file.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum SettingsEditStatus {
    /** The {@code changed} variant. */
    CHANGED("changed"),
    /** The {@code unchanged} variant. */
    UNCHANGED("unchanged"),
    /** The {@code invalid} variant. */
    INVALID("invalid");

    private final String value;
    SettingsEditStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SettingsEditStatus fromValue(String value) {
        for (SettingsEditStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SettingsEditStatus value: " + value);
    }
}
