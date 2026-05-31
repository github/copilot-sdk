/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Vote direction (vote only)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionRequestMemoryDirection {
    /** The {@code upvote} variant. */
    UPVOTE("upvote"),
    /** The {@code downvote} variant. */
    DOWNVOTE("downvote");

    private final String value;
    PermissionRequestMemoryDirection(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionRequestMemoryDirection fromValue(String value) {
        for (PermissionRequestMemoryDirection v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionRequestMemoryDirection value: " + value);
    }
}
