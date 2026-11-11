/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Shared HTTP engine failure categories. HTTP response statuses are not errors in this enumeration.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum FetchTransportErrorKind {
    /** The {@code request} variant. */
    REQUEST("request"),
    /** The {@code timeout} variant. */
    TIMEOUT("timeout"),
    /** The {@code tunnel} variant. */
    TUNNEL("tunnel"),
    /** The {@code proxy_authentication} variant. */
    PROXY_AUTHENTICATION("proxy_authentication"),
    /** The {@code request_configuration} variant. */
    REQUEST_CONFIGURATION("request_configuration"),
    /** The {@code response_body_limit} variant. */
    RESPONSE_BODY_LIMIT("response_body_limit"),
    /** The {@code redirect} variant. */
    REDIRECT("redirect"),
    /** The {@code canceled} variant. */
    CANCELED("canceled"),
    /** The {@code stream_closed} variant. */
    STREAM_CLOSED("stream_closed");

    private final String value;
    FetchTransportErrorKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FetchTransportErrorKind fromValue(String value) {
        for (FetchTransportErrorKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FetchTransportErrorKind value: " + value);
    }
}
