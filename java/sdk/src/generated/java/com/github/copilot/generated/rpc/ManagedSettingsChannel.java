/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * A channel accepted by managedSettings.compose.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ManagedSettingsChannel {
    /** The {@code device} variant. */
    DEVICE("device"),
    /** The {@code server} variant. */
    SERVER("server"),
    /** The {@code policyHelper} variant. */
    POLICYHELPER("policyHelper");

    private final String value;
    ManagedSettingsChannel(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ManagedSettingsChannel fromValue(String value) {
        for (ManagedSettingsChannel v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ManagedSettingsChannel value: " + value);
    }
}
