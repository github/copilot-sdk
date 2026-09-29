/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Terminal status reached by a workflow execution attempt.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SystemNotificationWorkflowCompletedStatus {
    /** The {@code completed} variant. */
    COMPLETED("completed"),
    /** The {@code halted} variant. */
    HALTED("halted"),
    /** The {@code paused} variant. */
    PAUSED("paused"),
    /** The {@code cancelled} variant. */
    CANCELLED("cancelled"),
    /** The {@code error} variant. */
    ERROR("error");

    private final String value;
    SystemNotificationWorkflowCompletedStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SystemNotificationWorkflowCompletedStatus fromValue(String value) {
        for (SystemNotificationWorkflowCompletedStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SystemNotificationWorkflowCompletedStatus value: " + value);
    }
}
