/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Units explicitly reported by a quota provider.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ProviderQuotaUnit {
    /** The {@code ai_credits} variant. */
    AI_CREDITS("ai_credits"),
    /** The {@code requests} variant. */
    REQUESTS("requests"),
    /** The {@code tokens} variant. */
    TOKENS("tokens"),
    /** The {@code unknown} variant. */
    UNKNOWN("unknown");

    private final String value;
    ProviderQuotaUnit(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ProviderQuotaUnit fromValue(String value) {
        for (ProviderQuotaUnit v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ProviderQuotaUnit value: " + value);
    }
}
