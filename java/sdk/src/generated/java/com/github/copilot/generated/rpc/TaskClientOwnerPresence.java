/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Presence of the task's bound join.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum TaskClientOwnerPresence {
    /** The {@code connected} variant. */
    CONNECTED("connected"),
    /** The {@code disconnected} variant. */
    DISCONNECTED("disconnected");

    private final String value;
    TaskClientOwnerPresence(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static TaskClientOwnerPresence fromValue(String value) {
        for (TaskClientOwnerPresence v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown TaskClientOwnerPresence value: " + value);
    }
}
