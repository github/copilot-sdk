/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Current lifecycle status of the task
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum TaskStatus {
    /** The {@code running} variant. */
    RUNNING("running"),
    /** The {@code idle} variant. */
    IDLE("idle"),
    /** The {@code completed} variant. */
    COMPLETED("completed"),
    /** The {@code failed} variant. */
    FAILED("failed"),
    /** The {@code cancelled} variant. */
    CANCELLED("cancelled");

    private final String value;
    TaskStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static TaskStatus fromValue(String value) {
        for (TaskStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown TaskStatus value: " + value);
    }
}
