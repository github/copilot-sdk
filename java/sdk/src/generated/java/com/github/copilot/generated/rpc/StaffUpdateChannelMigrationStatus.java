/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * What the one-time staff update-channel migration did.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum StaffUpdateChannelMigrationStatus {
    /** The {@code ineligible} variant. */
    INELIGIBLE("ineligible"),
    /** The {@code current} variant. */
    CURRENT("current"),
    /** The {@code migrated} variant. */
    MIGRATED("migrated"),
    /** The {@code completed} variant. */
    COMPLETED("completed"),
    /** The {@code excluded-experimental} variant. */
    EXCLUDED_EXPERIMENTAL("excluded-experimental");

    private final String value;
    StaffUpdateChannelMigrationStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static StaffUpdateChannelMigrationStatus fromValue(String value) {
        for (StaffUpdateChannelMigrationStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown StaffUpdateChannelMigrationStatus value: " + value);
    }
}
