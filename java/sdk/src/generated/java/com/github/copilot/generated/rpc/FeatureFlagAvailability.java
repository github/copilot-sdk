/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * The population a feature flag is declared for, before overrides.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum FeatureFlagAvailability {
    /** The {@code on} variant. */
    ON("on"),
    /** The {@code team} variant. */
    TEAM("team"),
    /** The {@code staff-and-experimental} variant. */
    STAFF_AND_EXPERIMENTAL("staff-and-experimental"),
    /** The {@code staff-or-experimental} variant. */
    STAFF_OR_EXPERIMENTAL("staff-or-experimental"),
    /** The {@code staff} variant. */
    STAFF("staff"),
    /** The {@code staff-github} variant. */
    STAFF_GITHUB("staff-github"),
    /** The {@code staff-microsoft} variant. */
    STAFF_MICROSOFT("staff-microsoft"),
    /** The {@code experimental} variant. */
    EXPERIMENTAL("experimental"),
    /** The {@code off} variant. */
    OFF("off");

    private final String value;
    FeatureFlagAvailability(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FeatureFlagAvailability fromValue(String value) {
        for (FeatureFlagAvailability v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FeatureFlagAvailability value: " + value);
    }
}
