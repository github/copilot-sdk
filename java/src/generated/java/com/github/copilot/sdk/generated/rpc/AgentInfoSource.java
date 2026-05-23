/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Where the agent definition was loaded from
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AgentInfoSource {
    /** The {@code user} variant. */
    USER("user"),
    /** The {@code project} variant. */
    PROJECT("project"),
    /** The {@code inherited} variant. */
    INHERITED("inherited"),
    /** The {@code remote} variant. */
    REMOTE("remote"),
    /** The {@code plugin} variant. */
    PLUGIN("plugin"),
    /** The {@code builtin} variant. */
    BUILTIN("builtin");

    private final String value;
    AgentInfoSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AgentInfoSource fromValue(String value) {
        for (AgentInfoSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AgentInfoSource value: " + value);
    }
}
