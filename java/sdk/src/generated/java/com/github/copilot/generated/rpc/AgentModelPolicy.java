/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Whether configured models are advisory preferences or required constraints
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AgentModelPolicy {
    /** The {@code preferred} variant. */
    PREFERRED("preferred"),
    /** The {@code required} variant. */
    REQUIRED("required");

    private final String value;
    AgentModelPolicy(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AgentModelPolicy fromValue(String value) {
        for (AgentModelPolicy v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AgentModelPolicy value: " + value);
    }
}
