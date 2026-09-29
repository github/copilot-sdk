/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Why this exact worker admission was made.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum WorkerAdmissionKind {
    /** The {@code queued_input} variant. */
    QUEUED_INPUT("queued_input"),
    /** The {@code system_continuation} variant. */
    SYSTEM_CONTINUATION("system_continuation");

    private final String value;
    WorkerAdmissionKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static WorkerAdmissionKind fromValue(String value) {
        for (WorkerAdmissionKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown WorkerAdmissionKind value: " + value);
    }
}
