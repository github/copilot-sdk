/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum SessionFeatureFlagsMode {
    /** The {@code local} variant. */
    LOCAL("local"),
    /** The {@code cli} variant. */
    CLI("cli");

    private final String value;
    SessionFeatureFlagsMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionFeatureFlagsMode fromValue(String value) {
        for (SessionFeatureFlagsMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionFeatureFlagsMode value: " + value);
    }
}
