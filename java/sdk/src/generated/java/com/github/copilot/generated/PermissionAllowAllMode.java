/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Allow-all mode for the session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionAllowAllMode {
    /** The {@code off} variant. */
    OFF("off"),
    /** The {@code on} variant. */
    ON("on"),
    /** The {@code auto} variant. */
    AUTO("auto");

    private final String value;
    PermissionAllowAllMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionAllowAllMode fromValue(String value) {
        for (PermissionAllowAllMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionAllowAllMode value: " + value);
    }
}
