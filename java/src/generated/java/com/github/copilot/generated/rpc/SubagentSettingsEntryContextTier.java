/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Context tier override for matching subagents
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SubagentSettingsEntryContextTier {
    /** The {@code inherit} variant. */
    INHERIT("inherit"),
    /** The {@code default} variant. */
    DEFAULT("default"),
    /** The {@code long_context} variant. */
    LONG_CONTEXT("long_context");

    private final String value;
    SubagentSettingsEntryContextTier(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SubagentSettingsEntryContextTier fromValue(String value) {
        for (SubagentSettingsEntryContextTier v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SubagentSettingsEntryContextTier value: " + value);
    }
}
