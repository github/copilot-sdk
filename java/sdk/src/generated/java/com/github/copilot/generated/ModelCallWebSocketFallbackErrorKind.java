/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Normalized cause of the WebSocket failure that triggered the HTTP fallback
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelCallWebSocketFallbackErrorKind {
    /** The {@code dns} variant. */
    DNS("dns"),
    /** The {@code tls} variant. */
    TLS("tls"),
    /** The {@code timeout} variant. */
    TIMEOUT("timeout"),
    /** The {@code connection_refused} variant. */
    CONNECTION_REFUSED("connection_refused"),
    /** The {@code connection_reset} variant. */
    CONNECTION_RESET("connection_reset"),
    /** The {@code closed_by_peer} variant. */
    CLOSED_BY_PEER("closed_by_peer"),
    /** The {@code closed_locally} variant. */
    CLOSED_LOCALLY("closed_locally"),
    /** The {@code not_connected} variant. */
    NOT_CONNECTED("not_connected"),
    /** The {@code http_status} variant. */
    HTTP_STATUS("http_status"),
    /** The {@code protocol} variant. */
    PROTOCOL("protocol"),
    /** The {@code configuration} variant. */
    CONFIGURATION("configuration"),
    /** The {@code other} variant. */
    OTHER("other");

    private final String value;
    ModelCallWebSocketFallbackErrorKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelCallWebSocketFallbackErrorKind fromValue(String value) {
        for (ModelCallWebSocketFallbackErrorKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelCallWebSocketFallbackErrorKind value: " + value);
    }
}
