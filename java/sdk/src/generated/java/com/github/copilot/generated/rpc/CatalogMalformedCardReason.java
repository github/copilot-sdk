/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * How a card failed validation
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogMalformedCardReason {
    /** The {@code invalid-json} variant. */
    INVALID_JSON("invalid-json"),
    /** The {@code schema-violation} variant. */
    SCHEMA_VIOLATION("schema-violation"),
    /** The {@code unsupported-media-type} variant. */
    UNSUPPORTED_MEDIA_TYPE("unsupported-media-type"),
    /** The {@code missing-required-field} variant. */
    MISSING_REQUIRED_FIELD("missing-required-field"),
    /** The {@code size-limit-exceeded} variant. */
    SIZE_LIMIT_EXCEEDED("size-limit-exceeded");

    private final String value;
    CatalogMalformedCardReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogMalformedCardReason fromValue(String value) {
        for (CatalogMalformedCardReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogMalformedCardReason value: " + value);
    }
}
