/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * API endpoint used for this model call, matching CAPI supported_endpoints vocabulary
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AssistantUsageApiEndpoint {
    /** The {@code /chat/completions} variant. */
    CHAT_COMPLETIONS("/chat/completions"),
    /** The {@code /v1/messages} variant. */
    V1_MESSAGES("/v1/messages"),
    /** The {@code /responses} variant. */
    RESPONSES("/responses"),
    /** The {@code ws:/responses} variant. */
    WS_RESPONSES("ws:/responses");

    private final String value;
    AssistantUsageApiEndpoint(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AssistantUsageApiEndpoint fromValue(String value) {
        for (AssistantUsageApiEndpoint v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AssistantUsageApiEndpoint value: " + value);
    }
}
