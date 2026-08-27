/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Which wire-contract rule an upstream response broke
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogContractViolationReason {
    /** The {@code both-url-and-data} variant. */
    BOTH_URL_AND_DATA("both-url-and-data"),
    /** The {@code neither-url-nor-data} variant. */
    NEITHER_URL_NOR_DATA("neither-url-nor-data"),
    /** The {@code duplicate-identity} variant. */
    DUPLICATE_IDENTITY("duplicate-identity"),
    /** The {@code unknown-media-type} variant. */
    UNKNOWN_MEDIA_TYPE("unknown-media-type");

    private final String value;
    CatalogContractViolationReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogContractViolationReason fromValue(String value) {
        for (CatalogContractViolationReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogContractViolationReason value: " + value);
    }
}
