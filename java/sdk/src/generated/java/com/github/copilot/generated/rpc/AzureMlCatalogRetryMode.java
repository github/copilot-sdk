/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * How catalog discovery handles a recent sign-in failure.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AzureMlCatalogRetryMode {
    /** The {@code background} variant. */
    BACKGROUND("background"),
    /** The {@code explicit} variant. */
    EXPLICIT("explicit");

    private final String value;
    AzureMlCatalogRetryMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AzureMlCatalogRetryMode fromValue(String value) {
        for (AzureMlCatalogRetryMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AzureMlCatalogRetryMode value: " + value);
    }
}
