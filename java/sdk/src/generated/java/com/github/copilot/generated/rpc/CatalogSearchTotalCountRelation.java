/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Relationship of the backend-reported count to the complete query result set.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogSearchTotalCountRelation {
    /** The {@code unknown} variant. */
    UNKNOWN("unknown");

    private final String value;
    CatalogSearchTotalCountRelation(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogSearchTotalCountRelation fromValue(String value) {
        for (CatalogSearchTotalCountRelation v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogSearchTotalCountRelation value: " + value);
    }
}
