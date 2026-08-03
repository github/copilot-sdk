/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Direction to page through the session's persisted event history. 'forward' pages from the cursor toward newer events; 'backward' returns the newest window first (tail-first) and pages toward older events. Events within a returned batch are always chronological (oldest-to-newest), even for a backward read.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum EventsReadDirection {
    /** The {@code forward} variant. */
    FORWARD("forward"),
    /** The {@code backward} variant. */
    BACKWARD("backward");

    private final String value;
    EventsReadDirection(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static EventsReadDirection fromValue(String value) {
        for (EventsReadDirection v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown EventsReadDirection value: " + value);
    }
}
