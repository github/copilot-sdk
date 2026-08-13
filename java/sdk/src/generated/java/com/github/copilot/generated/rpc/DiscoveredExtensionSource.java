/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Persisted extension discovery source
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum DiscoveredExtensionSource {
    /** The {@code user} variant. */
    USER("user"),
    /** The {@code plugin} variant. */
    PLUGIN("plugin");

    private final String value;
    DiscoveredExtensionSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static DiscoveredExtensionSource fromValue(String value) {
        for (DiscoveredExtensionSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown DiscoveredExtensionSource value: " + value);
    }
}
