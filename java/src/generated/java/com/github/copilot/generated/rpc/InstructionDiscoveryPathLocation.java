/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Which tier this target belongs to
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum InstructionDiscoveryPathLocation {
    /** The {@code user} variant. */
    USER("user"),
    /** The {@code repository} variant. */
    REPOSITORY("repository"),
    /** The {@code working-directory} variant. */
    WORKING_DIRECTORY("working-directory"),
    /** The {@code plugin} variant. */
    PLUGIN("plugin");

    private final String value;
    InstructionDiscoveryPathLocation(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static InstructionDiscoveryPathLocation fromValue(String value) {
        for (InstructionDiscoveryPathLocation v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown InstructionDiscoveryPathLocation value: " + value);
    }
}
