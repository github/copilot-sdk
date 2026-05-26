/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Signal to send (default: SIGTERM)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ShellKillSignal {
    /** The {@code SIGTERM} variant. */
    SIGTERM("SIGTERM"),
    /** The {@code SIGKILL} variant. */
    SIGKILL("SIGKILL"),
    /** The {@code SIGINT} variant. */
    SIGINT("SIGINT");

    private final String value;
    ShellKillSignal(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ShellKillSignal fromValue(String value) {
        for (ShellKillSignal v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ShellKillSignal value: " + value);
    }
}
