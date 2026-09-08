/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Why a presented handle was rejected
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogHandleRejectionReason {
    /** The {@code invalid} variant. */
    INVALID("invalid"),
    /** The {@code stale} variant. */
    STALE("stale"),
    /** The {@code replayed} variant. */
    REPLAYED("replayed"),
    /** The {@code foreign} variant. */
    FOREIGN("foreign");

    private final String value;
    CatalogHandleRejectionReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogHandleRejectionReason fromValue(String value) {
        for (CatalogHandleRejectionReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogHandleRejectionReason value: " + value);
    }
}
