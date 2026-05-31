/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Whether the agent completed successfully or failed
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SystemNotificationAgentCompletedStatus {
    /** The {@code completed} variant. */
    COMPLETED("completed"),
    /** The {@code failed} variant. */
    FAILED("failed");

    private final String value;
    SystemNotificationAgentCompletedStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SystemNotificationAgentCompletedStatus fromValue(String value) {
        for (SystemNotificationAgentCompletedStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SystemNotificationAgentCompletedStatus value: " + value);
    }
}
