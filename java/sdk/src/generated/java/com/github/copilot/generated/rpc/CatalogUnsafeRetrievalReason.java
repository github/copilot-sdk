/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Which hardened-fetch control refused a retrieval
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogUnsafeRetrievalReason {
    /** The {@code blocked-scheme} variant. */
    BLOCKED_SCHEME("blocked-scheme"),
    /** The {@code credentials-in-url} variant. */
    CREDENTIALS_IN_URL("credentials-in-url"),
    /** The {@code blocked-address} variant. */
    BLOCKED_ADDRESS("blocked-address"),
    /** The {@code redirect-to-blocked-address} variant. */
    REDIRECT_TO_BLOCKED_ADDRESS("redirect-to-blocked-address"),
    /** The {@code proxy-rejected} variant. */
    PROXY_REJECTED("proxy-rejected"),
    /** The {@code host-not-permitted} variant. */
    HOST_NOT_PERMITTED("host-not-permitted");

    private final String value;
    CatalogUnsafeRetrievalReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogUnsafeRetrievalReason fromValue(String value) {
        for (CatalogUnsafeRetrievalReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogUnsafeRetrievalReason value: " + value);
    }
}
