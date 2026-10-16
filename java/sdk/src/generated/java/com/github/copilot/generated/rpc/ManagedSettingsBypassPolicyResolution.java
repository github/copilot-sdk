/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * What the managed-policy resolution said about bypass-permissions mode. `unknown` is a third answer rather than a missing one, and the caller must send it explicitly when it has no verdict.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum ManagedSettingsBypassPolicyResolution {
    /** The {@code allowed} variant. */
    ALLOWED("allowed"),
    /** The {@code disabled} variant. */
    DISABLED("disabled"),
    /** The {@code unknown} variant. */
    UNKNOWN("unknown");

    private final String value;
    ManagedSettingsBypassPolicyResolution(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ManagedSettingsBypassPolicyResolution fromValue(String value) {
        for (ManagedSettingsBypassPolicyResolution v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ManagedSettingsBypassPolicyResolution value: " + value);
    }
}
