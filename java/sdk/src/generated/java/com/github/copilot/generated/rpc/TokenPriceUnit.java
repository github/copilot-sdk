/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Unit the catalog publishes a model's token prices in. The numeric fields of ModelBillingTokenPrices carry no unit of their own, so a client that renders a price needs this to scale it.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum TokenPriceUnit {
    /** The {@code ai_credits} variant. */
    AI_CREDITS("ai_credits"),
    /** The {@code nano_aiu} variant. */
    NANO_AIU("nano_aiu");

    private final String value;
    TokenPriceUnit(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static TokenPriceUnit fromValue(String value) {
        for (TokenPriceUnit v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown TokenPriceUnit value: " + value);
    }
}
