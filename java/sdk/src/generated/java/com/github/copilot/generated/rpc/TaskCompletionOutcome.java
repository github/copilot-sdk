/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Semantic result of evaluating a task completion request
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum TaskCompletionOutcome {
    /** The {@code completed} variant. */
    COMPLETED("completed"),
    /** The {@code continue} variant. */
    CONTINUE("continue"),
    /** The {@code blocked} variant. */
    BLOCKED("blocked");

    private final String value;
    TaskCompletionOutcome(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static TaskCompletionOutcome fromValue(String value) {
        for (TaskCompletionOutcome v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown TaskCompletionOutcome value: " + value);
    }
}
