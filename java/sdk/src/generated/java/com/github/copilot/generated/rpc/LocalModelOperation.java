/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Active acquisition or removal stage.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum LocalModelOperation {
    /** The {@code installing} variant. */
    INSTALLING("installing"),
    /** The {@code probing} variant. */
    PROBING("probing"),
    /** The {@code downloading} variant. */
    DOWNLOADING("downloading"),
    /** The {@code registering} variant. */
    REGISTERING("registering"),
    /** The {@code deleting} variant. */
    DELETING("deleting");

    private final String value;
    LocalModelOperation(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static LocalModelOperation fromValue(String value) {
        for (LocalModelOperation v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown LocalModelOperation value: " + value);
    }
}
