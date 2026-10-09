/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * What retrying did for one plugin required by managed settings.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ManagedPluginRetryStatus {
    /** The {@code installed} variant. */
    INSTALLED("installed"),
    /** The {@code updated} variant. */
    UPDATED("updated"),
    /** The {@code already_present} variant. */
    ALREADY_PRESENT("already_present"),
    /** The {@code failed} variant. */
    FAILED("failed"),
    /** The {@code deferred} variant. */
    DEFERRED("deferred"),
    /** The {@code not_required} variant. */
    NOT_REQUIRED("not_required");

    private final String value;
    ManagedPluginRetryStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ManagedPluginRetryStatus fromValue(String value) {
        for (ManagedPluginRetryStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ManagedPluginRetryStatus value: " + value);
    }
}
