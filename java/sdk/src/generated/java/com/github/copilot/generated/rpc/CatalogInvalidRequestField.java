/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Which request field was rejected locally or by the catalog authority
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogInvalidRequestField {
    /** The {@code query} variant. */
    QUERY("query"),
    /** The {@code limit} variant. */
    LIMIT("limit"),
    /** The {@code kinds} variant. */
    KINDS("kinds"),
    /** The {@code contract} variant. */
    CONTRACT("contract"),
    /** The {@code source} variant. */
    SOURCE("source"),
    /** The {@code card} variant. */
    CARD("card"),
    /** The {@code scope} variant. */
    SCOPE("scope"),
    /** The {@code page} variant. */
    PAGE("page"),
    /** The {@code sessionId} variant. */
    SESSIONID("sessionId"),
    /** The {@code selectionRef} variant. */
    SELECTIONREF("selectionRef"),
    /** The {@code outcome} variant. */
    OUTCOME("outcome");

    private final String value;
    CatalogInvalidRequestField(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogInvalidRequestField fromValue(String value) {
        for (CatalogInvalidRequestField v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogInvalidRequestField value: " + value);
    }
}
