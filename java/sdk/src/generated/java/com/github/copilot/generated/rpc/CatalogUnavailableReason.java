/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Why a catalog operation is not available on this runtime
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogUnavailableReason {
    /** The {@code search-unavailable} variant. */
    SEARCH_UNAVAILABLE("search-unavailable"),
    /** The {@code planning-unavailable} variant. */
    PLANNING_UNAVAILABLE("planning-unavailable"),
    /** The {@code authority-not-configured} variant. */
    AUTHORITY_NOT_CONFIGURED("authority-not-configured"),
    /** The {@code disabled-by-policy} variant. */
    DISABLED_BY_POLICY("disabled-by-policy");

    private final String value;
    CatalogUnavailableReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogUnavailableReason fromValue(String value) {
        for (CatalogUnavailableReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogUnavailableReason value: " + value);
    }
}
