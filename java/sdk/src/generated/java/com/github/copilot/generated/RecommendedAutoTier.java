/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Auto preferences that Copilot API can recommend.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum RecommendedAutoTier {
    /** The {@code efficiency} variant. */
    EFFICIENCY("efficiency"),
    /** The {@code balance} variant. */
    BALANCE("balance"),
    /** The {@code intelligence} variant. */
    INTELLIGENCE("intelligence");

    private final String value;
    RecommendedAutoTier(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static RecommendedAutoTier fromValue(String value) {
        for (RecommendedAutoTier v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown RecommendedAutoTier value: " + value);
    }
}
