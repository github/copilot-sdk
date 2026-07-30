/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * The current agent mode for this session (e.g., 'interactive', 'plan', 'autopilot')
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum MetadataSnapshotCurrentMode {
    /** The {@code interactive} variant. */
    INTERACTIVE("interactive"),
    /** The {@code plan} variant. */
    PLAN("plan"),
    /** The {@code autopilot} variant. */
    AUTOPILOT("autopilot");

    private final String value;
    MetadataSnapshotCurrentMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static MetadataSnapshotCurrentMode fromValue(String value) {
        for (MetadataSnapshotCurrentMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown MetadataSnapshotCurrentMode value: " + value);
    }
}
