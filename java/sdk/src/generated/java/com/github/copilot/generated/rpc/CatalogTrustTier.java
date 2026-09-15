/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Service-computed trust tier currently emitted by Agent Finder. It is independent of search score, popularity, and client-side ranking.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogTrustTier {
    /** The {@code T1} variant. */
    T1("T1"),
    /** The {@code T2} variant. */
    T2("T2");

    private final String value;
    CatalogTrustTier(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogTrustTier fromValue(String value) {
        for (CatalogTrustTier v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogTrustTier value: " + value);
    }
}
