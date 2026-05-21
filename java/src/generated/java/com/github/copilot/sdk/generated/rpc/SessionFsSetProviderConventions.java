/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Path conventions used by this filesystem
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionFsSetProviderConventions {
    /** The {@code windows} variant. */
    WINDOWS("windows"),
    /** The {@code posix} variant. */
    POSIX("posix");

    private final String value;
    SessionFsSetProviderConventions(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionFsSetProviderConventions fromValue(String value) {
        for (SessionFsSetProviderConventions v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionFsSetProviderConventions value: " + value);
    }
}
