/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * The type of operation performed on the plan file
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PlanChangedOperation {
    /** The {@code create} variant. */
    CREATE("create"),
    /** The {@code update} variant. */
    UPDATE("update"),
    /** The {@code delete} variant. */
    DELETE("delete");

    private final String value;
    PlanChangedOperation(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PlanChangedOperation fromValue(String value) {
        for (PlanChangedOperation v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PlanChangedOperation value: " + value);
    }
}
