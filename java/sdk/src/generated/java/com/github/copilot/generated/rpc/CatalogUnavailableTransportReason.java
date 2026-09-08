/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Why no usable transport could be offered
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogUnavailableTransportReason {
    /** The {@code no-eligible-transport} variant. */
    NO_ELIGIBLE_TRANSPORT("no-eligible-transport"),
    /** The {@code transport-not-supported} variant. */
    TRANSPORT_NOT_SUPPORTED("transport-not-supported"),
    /** The {@code remote-enumeration-unavailable} variant. */
    REMOTE_ENUMERATION_UNAVAILABLE("remote-enumeration-unavailable");

    private final String value;
    CatalogUnavailableTransportReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogUnavailableTransportReason fromValue(String value) {
        for (CatalogUnavailableTransportReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogUnavailableTransportReason value: " + value);
    }
}
