/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Schema version of the catalogue trust snapshot envelope
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogTrustSnapshotSchemaVersion {
    /** The {@code v1} variant. */
    V1("v1");

    private final String value;
    CatalogTrustSnapshotSchemaVersion(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogTrustSnapshotSchemaVersion fromValue(String value) {
        for (CatalogTrustSnapshotSchemaVersion v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogTrustSnapshotSchemaVersion value: " + value);
    }
}
