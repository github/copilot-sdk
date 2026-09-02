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
public enum AutoTier {

    /** Prioritize efficiency. */
    EFFICIENCY("efficiency"),

    /** Balance efficiency and intelligence. */
    BALANCE("balance"),

    /** Prioritize intelligence. */
    INTELLIGENCE("intelligence");

    private final String value;

    AutoTier(String value) {
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
     *             if the value does not match a known routing tier
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
        throw new IllegalArgumentException("Unknown AutoTier value: " + value);
    }
}
