/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Context tier for models with tiered pricing. The session uses this to derive effective `modelCapabilitiesOverrides` so compaction, truncation, token display, and request limits honor the selected tier.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum OptionsUpdateContextTier {
    /** The {@code default} variant. */
    DEFAULT("default"),
    /** The {@code long_context} variant. */
    LONG_CONTEXT("long_context");

    private final String value;
    OptionsUpdateContextTier(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static OptionsUpdateContextTier fromValue(String value) {
        for (OptionsUpdateContextTier v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown OptionsUpdateContextTier value: " + value);
    }
}
