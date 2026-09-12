/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Authority-computed exposure eligibility, kept separate from tier. The current tier-only Agent Finder response maps to `unknown`, never to a locally inferred eligibility.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogTrustEligibility {
    /** The {@code default} variant. */
    DEFAULT("default"),
    /** The {@code expanded} variant. */
    EXPANDED("expanded"),
    /** The {@code hidden} variant. */
    HIDDEN("hidden"),
    /** The {@code unknown} variant. */
    UNKNOWN("unknown");

    private final String value;
    CatalogTrustEligibility(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogTrustEligibility fromValue(String value) {
        for (CatalogTrustEligibility v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogTrustEligibility value: " + value);
    }
}
