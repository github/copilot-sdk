/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Controls automatic non-interactive profile loading where supported. Explicit initScripts are unaffected.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ShellInitProfile {
    /** The {@code none} variant. */
    NONE("none"),
    /** The {@code non-interactive} variant. */
    NON_INTERACTIVE("non-interactive");

    private final String value;
    ShellInitProfile(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ShellInitProfile fromValue(String value) {
        for (ShellInitProfile v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ShellInitProfile value: " + value);
    }
}
