/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Categorised network failure, low cardinality so it can be aggregated without carrying a URL
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogNetworkFailureReason {
    /** The {@code offline} variant. */
    OFFLINE("offline"),
    /** The {@code dns} variant. */
    DNS("dns"),
    /** The {@code timeout} variant. */
    TIMEOUT("timeout"),
    /** The {@code tls} variant. */
    TLS("tls"),
    /** The {@code connection-refused} variant. */
    CONNECTION_REFUSED("connection-refused"),
    /** The {@code http-status} variant. */
    HTTP_STATUS("http-status"),
    /** The {@code response-too-large} variant. */
    RESPONSE_TOO_LARGE("response-too-large"),
    /** The {@code redirect-rejected} variant. */
    REDIRECT_REJECTED("redirect-rejected");

    private final String value;
    CatalogNetworkFailureReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogNetworkFailureReason fromValue(String value) {
        for (CatalogNetworkFailureReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogNetworkFailureReason value: " + value);
    }
}
