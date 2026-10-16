/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * What the one-time staff log-level migration did.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum StaffLogLevelMigrationStatus {
    /** The {@code ineligible} variant. */
    INELIGIBLE("ineligible"),
    /** The {@code current} variant. */
    CURRENT("current"),
    /** The {@code refreshed} variant. */
    REFRESHED("refreshed");

    private final String value;
    StaffLogLevelMigrationStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static StaffLogLevelMigrationStatus fromValue(String value) {
        for (StaffLogLevelMigrationStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown StaffLogLevelMigrationStatus value: " + value);
    }
}
