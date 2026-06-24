/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Permission posture for the new session. 'yolo' requires the controller-local session to currently be in allow-all mode.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AgentRegistrySpawnPermissionMode {
    /** The {@code default} variant. */
    DEFAULT("default"),
    /** The {@code yolo} variant. */
    YOLO("yolo");

    private final String value;
    AgentRegistrySpawnPermissionMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AgentRegistrySpawnPermissionMode fromValue(String value) {
        for (AgentRegistrySpawnPermissionMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AgentRegistrySpawnPermissionMode value: " + value);
    }
}
