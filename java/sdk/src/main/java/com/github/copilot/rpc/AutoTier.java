/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Routing tier for the {@code auto} model with Auto mode V2.
 *
 * @see CapiSessionOptions#setAutoTier(AutoTier)
 */
public final class AutoTier {

    /** Prioritize efficiency. */
    public static final AutoTier EFFICIENCY = new AutoTier("efficiency");

    /** Balance efficiency and intelligence. */
    public static final AutoTier BALANCE = new AutoTier("balance");

    /** Prioritize intelligence. */
    public static final AutoTier INTELLIGENCE = new AutoTier("intelligence");

    /**
     * Integrator-only preset that optimizes for latency. Not a first-party GitHub
     * Copilot product preference.
     */
    public static final AutoTier FAST = new AutoTier("fast");

    private final String value;

    private AutoTier(String value) {
        this.value = value;
    }

    /**
     * Returns the JSON value for this routing tier.
     *
     * @return the string value used in JSON serialization
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Deserializes a JSON string into its routing tier.
     *
     * @param value
     *            the JSON string value
     * @return the matching tier, or {@code null} if value is {@code null}
     * @throws IllegalArgumentException
     *             if the value is not a routing identifier
     */
    @JsonCreator
    public static AutoTier fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (AutoTier tier : values()) {
            if (tier.value.equals(value)) {
                return tier;
            }
        }
        return new AutoTier(com.github.copilot.generated.rpc.AutoTier.fromValue(value).getValue());
    }

    /** Returns the known convenience values. @return known routing tiers */
    public static AutoTier[] values() {
        return new AutoTier[]{EFFICIENCY, BALANCE, INTELLIGENCE, FAST};
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AutoTier tier && value.equals(tier.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
