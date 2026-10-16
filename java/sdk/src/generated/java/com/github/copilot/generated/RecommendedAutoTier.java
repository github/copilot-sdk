/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Enabled Auto preferences that Copilot API can recommend.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class RecommendedAutoTier {
    /** The {@code efficiency} variant. */
    public static final RecommendedAutoTier EFFICIENCY = new RecommendedAutoTier("efficiency");
    /** The {@code balance} variant. */
    public static final RecommendedAutoTier BALANCE = new RecommendedAutoTier("balance");
    /** The {@code intelligence} variant. */
    public static final RecommendedAutoTier INTELLIGENCE = new RecommendedAutoTier("intelligence");

    private final String value;
    private RecommendedAutoTier(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static RecommendedAutoTier fromValue(String value) {
        if (value == null) return null;
        if (value.isEmpty() || value.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c)))
            throw new IllegalArgumentException("RecommendedAutoTier requires a routing identifier");
        for (RecommendedAutoTier v : values()) {
            if (v.value.equals(value)) return v;
        }
        return new RecommendedAutoTier(value);
    }
    /** Returns the known routing preferences. @return known values */
    public static RecommendedAutoTier[] values() { return new RecommendedAutoTier[] { EFFICIENCY, BALANCE, INTELLIGENCE }; }
    @Override public boolean equals(Object other) { return other instanceof RecommendedAutoTier v && value.equals(v.value); }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public String toString() { return value; }
}
