/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * For a `bypass_permissions_blocked` action, which permission-escalation primitive was refused
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ManagedSettingsEnforcedEscalation {
    /** The {@code allow_all} variant. */
    ALLOW_ALL("allow_all"),
    /** The {@code approve_all} variant. */
    APPROVE_ALL("approve_all"),
    /** The {@code auto_approval} variant. */
    AUTO_APPROVAL("auto_approval"),
    /** The {@code unrestricted_paths} variant. */
    UNRESTRICTED_PATHS("unrestricted_paths"),
    /** The {@code unrestricted_urls} variant. */
    UNRESTRICTED_URLS("unrestricted_urls");

    private final String value;
    ManagedSettingsEnforcedEscalation(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ManagedSettingsEnforcedEscalation fromValue(String value) {
        for (ManagedSettingsEnforcedEscalation v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ManagedSettingsEnforcedEscalation value: " + value);
    }
}
