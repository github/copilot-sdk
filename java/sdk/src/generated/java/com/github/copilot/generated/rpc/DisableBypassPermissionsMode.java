/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum DisableBypassPermissionsMode {
    /** The {@code disable} variant. */
    DISABLE("disable");

    private final String value;
    DisableBypassPermissionsMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static DisableBypassPermissionsMode fromValue(String value) {
        for (DisableBypassPermissionsMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown DisableBypassPermissionsMode value: " + value);
    }
}
