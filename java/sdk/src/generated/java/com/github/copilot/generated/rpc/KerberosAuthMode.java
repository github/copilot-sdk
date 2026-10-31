/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Selects whether Kerberos credentials come from native lookup with a client fallback, or only from the client.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum KerberosAuthMode {
    /** The {@code fallback} variant. */
    FALLBACK("fallback"),
    /** The {@code always} variant. */
    ALWAYS("always");

    private final String value;
    KerberosAuthMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static KerberosAuthMode fromValue(String value) {
        for (KerberosAuthMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown KerberosAuthMode value: " + value);
    }
}
