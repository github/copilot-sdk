/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Authoritative service connection state for one Connector.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ConnectorCatalogStatus {
    /** The {@code not_connected} variant. */
    NOT_CONNECTED("not_connected"),
    /** The {@code pending} variant. */
    PENDING("pending"),
    /** The {@code connected} variant. */
    CONNECTED("connected"),
    /** The {@code error} variant. */
    ERROR("error"),
    /** The {@code unknown} variant. */
    UNKNOWN("unknown");

    private final String value;
    ConnectorCatalogStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ConnectorCatalogStatus fromValue(String value) {
        for (ConnectorCatalogStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ConnectorCatalogStatus value: " + value);
    }
}
