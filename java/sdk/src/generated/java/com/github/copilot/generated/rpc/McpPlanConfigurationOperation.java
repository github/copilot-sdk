/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Whether a planned configuration change would create or modify an entry
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpPlanConfigurationOperation {
    /** The {@code add} variant. */
    ADD("add"),
    /** The {@code update} variant. */
    UPDATE("update");

    private final String value;
    McpPlanConfigurationOperation(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpPlanConfigurationOperation fromValue(String value) {
        for (McpPlanConfigurationOperation v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpPlanConfigurationOperation value: " + value);
    }
}
