/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Coarse lifecycle status of the foreground session
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AgentRegistryLiveTargetEntryStatus {
    /** The {@code working} variant. */
    WORKING("working"),
    /** The {@code waiting} variant. */
    WAITING("waiting"),
    /** The {@code done} variant. */
    DONE("done"),
    /** The {@code attention} variant. */
    ATTENTION("attention");

    private final String value;
    AgentRegistryLiveTargetEntryStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AgentRegistryLiveTargetEntryStatus fromValue(String value) {
        for (AgentRegistryLiveTargetEntryStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AgentRegistryLiveTargetEntryStatus value: " + value);
    }
}
