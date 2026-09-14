/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Lifecycle status of a client-owned task.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum TaskClientStatus {
    /** The {@code running} variant. */
    RUNNING("running"),
    /** The {@code idle} variant. */
    IDLE("idle"),
    /** The {@code completed} variant. */
    COMPLETED("completed"),
    /** The {@code failed} variant. */
    FAILED("failed"),
    /** The {@code cancelled} variant. */
    CANCELLED("cancelled"),
    /** The {@code orphaned} variant. */
    ORPHANED("orphaned");

    private final String value;
    TaskClientStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static TaskClientStatus fromValue(String value) {
        for (TaskClientStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown TaskClientStatus value: " + value);
    }
}
