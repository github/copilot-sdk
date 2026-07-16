/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Which channel supplied the effective enterprise managed settings (highest-authority present layer wins wholesale)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ManagedSettingsResolvedSource {
    /** The {@code server} variant. */
    SERVER("server"),
    /** The {@code device} variant. */
    DEVICE("device"),
    /** The {@code none} variant. */
    NONE("none");

    private final String value;
    ManagedSettingsResolvedSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ManagedSettingsResolvedSource fromValue(String value) {
        for (ManagedSettingsResolvedSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ManagedSettingsResolvedSource value: " + value);
    }
}
