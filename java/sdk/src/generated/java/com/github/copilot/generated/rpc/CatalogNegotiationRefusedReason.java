/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Why capability and protocol-version negotiation refused a caller
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogNegotiationRefusedReason {
    /** The {@code unsupported-protocol-version} variant. */
    UNSUPPORTED_PROTOCOL_VERSION("unsupported-protocol-version"),
    /** The {@code unsupported-capability} variant. */
    UNSUPPORTED_CAPABILITY("unsupported-capability");

    private final String value;
    CatalogNegotiationRefusedReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogNegotiationRefusedReason fromValue(String value) {
        for (CatalogNegotiationRefusedReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogNegotiationRefusedReason value: " + value);
    }
}
