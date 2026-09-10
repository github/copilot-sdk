/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Interactive condition blocking the main agent.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionMainAgentWaitReason {
    /** The {@code permission} variant. */
    PERMISSION("permission"),
    /** The {@code user_input} variant. */
    USER_INPUT("user_input");

    private final String value;
    SessionMainAgentWaitReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionMainAgentWaitReason fromValue(String value) {
        for (SessionMainAgentWaitReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionMainAgentWaitReason value: " + value);
    }
}
