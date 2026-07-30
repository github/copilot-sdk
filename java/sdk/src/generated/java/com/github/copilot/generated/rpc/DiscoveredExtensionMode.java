/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Effective extension loading and agent-management mode
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum DiscoveredExtensionMode {
    /** The {@code disabled} variant. */
    DISABLED("disabled"),
    /** The {@code load_only} variant. */
    LOAD_ONLY("load_only"),
    /** The {@code load_and_augment} variant. */
    LOAD_AND_AUGMENT("load_and_augment");

    private final String value;
    DiscoveredExtensionMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static DiscoveredExtensionMode fromValue(String value) {
        for (DiscoveredExtensionMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown DiscoveredExtensionMode value: " + value);
    }
}
