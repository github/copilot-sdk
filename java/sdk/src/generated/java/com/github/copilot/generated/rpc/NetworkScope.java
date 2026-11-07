/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Selects which traffic a client's network callbacks serve.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum NetworkScope {
    /** The {@code connection} variant. */
    CONNECTION("connection"),
    /** The {@code runtime} variant. */
    RUNTIME("runtime");

    private final String value;
    NetworkScope(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static NetworkScope fromValue(String value) {
        for (NetworkScope v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown NetworkScope value: " + value);
    }
}
