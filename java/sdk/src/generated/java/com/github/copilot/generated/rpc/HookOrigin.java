/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Configuration tier that contributed a discovered hook action.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum HookOrigin {
    /** The {@code user} variant. */
    USER("user"),
    /** The {@code repository} variant. */
    REPOSITORY("repository"),
    /** The {@code plugin} variant. */
    PLUGIN("plugin"),
    /** The {@code policy} variant. */
    POLICY("policy");

    private final String value;
    HookOrigin(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static HookOrigin fromValue(String value) {
        for (HookOrigin v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown HookOrigin value: " + value);
    }
}
