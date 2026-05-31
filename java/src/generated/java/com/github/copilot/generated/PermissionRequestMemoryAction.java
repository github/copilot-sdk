/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Whether this is a store or vote memory operation
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionRequestMemoryAction {
    /** The {@code store} variant. */
    STORE("store"),
    /** The {@code vote} variant. */
    VOTE("vote");

    private final String value;
    PermissionRequestMemoryAction(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionRequestMemoryAction fromValue(String value) {
        for (PermissionRequestMemoryAction v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionRequestMemoryAction value: " + value);
    }
}
