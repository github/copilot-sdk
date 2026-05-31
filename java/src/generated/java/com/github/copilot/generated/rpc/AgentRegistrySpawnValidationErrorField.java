/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Which parameter field was invalid. Omitted when the rejection is not field-specific.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AgentRegistrySpawnValidationErrorField {
    /** The {@code cwd} variant. */
    CWD("cwd"),
    /** The {@code name} variant. */
    NAME("name"),
    /** The {@code agentName} variant. */
    AGENTNAME("agentName"),
    /** The {@code model} variant. */
    MODEL("model"),
    /** The {@code permissionMode} variant. */
    PERMISSIONMODE("permissionMode");

    private final String value;
    AgentRegistrySpawnValidationErrorField(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AgentRegistrySpawnValidationErrorField fromValue(String value) {
        for (AgentRegistrySpawnValidationErrorField v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AgentRegistrySpawnValidationErrorField value: " + value);
    }
}
