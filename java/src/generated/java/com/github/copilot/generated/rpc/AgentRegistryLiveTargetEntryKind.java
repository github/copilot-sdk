/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Process kind tag for the registry entry
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AgentRegistryLiveTargetEntryKind {
    /** The {@code ui-server} variant. */
    UI_SERVER("ui-server"),
    /** The {@code managed-server} variant. */
    MANAGED_SERVER("managed-server");

    private final String value;
    AgentRegistryLiveTargetEntryKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AgentRegistryLiveTargetEntryKind fromValue(String value) {
        for (AgentRegistryLiveTargetEntryKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AgentRegistryLiveTargetEntryKind value: " + value);
    }
}
