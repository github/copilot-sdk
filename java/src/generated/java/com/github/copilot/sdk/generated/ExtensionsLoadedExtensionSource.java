/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * Discovery source
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ExtensionsLoadedExtensionSource {
    /** The {@code project} variant. */
    PROJECT("project"),
    /** The {@code user} variant. */
    USER("user");

    private final String value;
    ExtensionsLoadedExtensionSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ExtensionsLoadedExtensionSource fromValue(String value) {
        for (ExtensionsLoadedExtensionSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ExtensionsLoadedExtensionSource value: " + value);
    }
}
