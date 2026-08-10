/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * The category of runtime action that enterprise managed settings governed (blocked or capped)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ManagedSettingsEnforcedAction {
    /** The {@code bypass_permissions_blocked} variant. */
    BYPASS_PERMISSIONS_BLOCKED("bypass_permissions_blocked");

    private final String value;
    ManagedSettingsEnforcedAction(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ManagedSettingsEnforcedAction fromValue(String value) {
        for (ManagedSettingsEnforcedAction v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ManagedSettingsEnforcedAction value: " + value);
    }
}
