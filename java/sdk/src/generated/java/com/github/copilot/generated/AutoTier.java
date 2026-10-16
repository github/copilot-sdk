/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Extensible routing preference for the virtual `auto` model. New identifiers must be advertised and enabled by the provider. `fast` is an integrator-only latency preset.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AutoTier {
    /** The {@code efficiency} variant. */
    public static final AutoTier EFFICIENCY = new AutoTier("efficiency");
    /** The {@code balance} variant. */
    public static final AutoTier BALANCE = new AutoTier("balance");
    /** The {@code intelligence} variant. */
    public static final AutoTier INTELLIGENCE = new AutoTier("intelligence");
    /** The {@code fast} variant. */
    public static final AutoTier FAST = new AutoTier("fast");

    private final String value;
    private AutoTier(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AutoTier fromValue(String value) {
        if (value == null) return null;
        if (value.isEmpty() || value.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c)))
            throw new IllegalArgumentException("AutoTier requires a routing identifier");
        for (AutoTier v : values()) {
            if (v.value.equals(value)) return v;
        }
        return new AutoTier(value);
    }
    /** Returns the known routing preferences. @return known values */
    public static AutoTier[] values() { return new AutoTier[] { EFFICIENCY, BALANCE, INTELLIGENCE, FAST }; }
    @Override public boolean equals(Object other) { return other instanceof AutoTier v && value.equals(v.value); }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public String toString() { return value; }
}
