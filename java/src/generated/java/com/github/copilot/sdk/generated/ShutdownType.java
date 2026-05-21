/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * Whether the session ended normally ("routine") or due to a crash/fatal error ("error")
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ShutdownType {
    /** The {@code routine} variant. */
    ROUTINE("routine"),
    /** The {@code error} variant. */
    ERROR("error");

    private final String value;
    ShutdownType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ShutdownType fromValue(String value) {
        for (ShutdownType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ShutdownType value: " + value);
    }
}
