/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Severity of a diagnostic emitted while ingesting managed settings.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum ManagedSettingsLogLevel {
    /** The {@code debug} variant. */
    DEBUG("debug"),
    /** The {@code info} variant. */
    INFO("info"),
    /** The {@code warning} variant. */
    WARNING("warning"),
    /** The {@code error} variant. */
    ERROR("error");

    private final String value;
    ManagedSettingsLogLevel(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ManagedSettingsLogLevel fromValue(String value) {
        for (ManagedSettingsLogLevel v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ManagedSettingsLogLevel value: " + value);
    }
}
