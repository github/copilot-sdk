/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Routing preference used when the session model is `auto`.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AutoTier {
    /** The {@code efficiency} variant. */
    EFFICIENCY("efficiency"),
    /** The {@code balance} variant. */
    BALANCE("balance"),
    /** The {@code intelligence} variant. */
    INTELLIGENCE("intelligence");

    private final String value;
    AutoTier(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AutoTier fromValue(String value) {
        for (AutoTier v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AutoTier value: " + value);
    }
}
