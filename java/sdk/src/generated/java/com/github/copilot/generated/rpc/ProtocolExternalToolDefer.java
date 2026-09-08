/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Controls whether the runtime may defer loading an external tool definition.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ProtocolExternalToolDefer {
    /** The {@code auto} variant. */
    AUTO("auto"),
    /** The {@code never} variant. */
    NEVER("never");

    private final String value;
    ProtocolExternalToolDefer(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ProtocolExternalToolDefer fromValue(String value) {
        for (ProtocolExternalToolDefer v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ProtocolExternalToolDefer value: " + value);
    }
}
