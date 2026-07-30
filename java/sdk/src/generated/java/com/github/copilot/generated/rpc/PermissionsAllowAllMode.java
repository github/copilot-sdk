/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Current or requested allow-all mode.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionsAllowAllMode {
    /** The {@code off} variant. */
    OFF("off"),
    /** The {@code on} variant. */
    ON("on"),
    /** The {@code auto} variant. */
    AUTO("auto");

    private final String value;
    PermissionsAllowAllMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionsAllowAllMode fromValue(String value) {
        for (PermissionsAllowAllMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionsAllowAllMode value: " + value);
    }
}
