/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Why a WebSocket-capable model call was carried by the HTTP fallback
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelCallWebSocketFallbackReason {
    /** The {@code connect_failed} variant. */
    CONNECT_FAILED("connect_failed"),
    /** The {@code connection_unavailable} variant. */
    CONNECTION_UNAVAILABLE("connection_unavailable"),
    /** The {@code send_failed} variant. */
    SEND_FAILED("send_failed"),
    /** The {@code api_error} variant. */
    API_ERROR("api_error"),
    /** The {@code transport_failed} variant. */
    TRANSPORT_FAILED("transport_failed");

    private final String value;
    ModelCallWebSocketFallbackReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelCallWebSocketFallbackReason fromValue(String value) {
        for (ModelCallWebSocketFallbackReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelCallWebSocketFallbackReason value: " + value);
    }
}
