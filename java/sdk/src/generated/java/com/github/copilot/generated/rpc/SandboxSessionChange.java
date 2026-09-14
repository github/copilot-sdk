/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * A session-scoped sandbox transition applied while handling a slash command
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SandboxSessionChange {
    /** The {@code disabled} variant. */
    DISABLED("disabled"),
    /** The {@code restored} variant. */
    RESTORED("restored");

    private final String value;
    SandboxSessionChange(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SandboxSessionChange fromValue(String value) {
        for (SandboxSessionChange v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SandboxSessionChange value: " + value);
    }
}
