/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Error classification
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionFsErrorCode {
    /** The {@code ENOENT} variant. */
    ENOENT("ENOENT"),
    /** The {@code UNKNOWN} variant. */
    UNKNOWN("UNKNOWN");

    private final String value;
    SessionFsErrorCode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionFsErrorCode fromValue(String value) {
        for (SessionFsErrorCode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionFsErrorCode value: " + value);
    }
}
