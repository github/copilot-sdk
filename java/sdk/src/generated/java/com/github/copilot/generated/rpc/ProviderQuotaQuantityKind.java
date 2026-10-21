/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Interpretation permitted for independently reported quantities.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ProviderQuotaQuantityKind {
    /** The {@code authoritative_budget} variant. */
    AUTHORITATIVE_BUDGET("authoritative_budget"),
    /** The {@code advisory_balance} variant. */
    ADVISORY_BALANCE("advisory_balance"),
    /** The {@code none} variant. */
    NONE("none");

    private final String value;
    ProviderQuotaQuantityKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ProviderQuotaQuantityKind fromValue(String value) {
        for (ProviderQuotaQuantityKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ProviderQuotaQuantityKind value: " + value);
    }
}
