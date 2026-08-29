/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Terminal status a factory run committed. A settled run is never `pending` or `running`, so those two members of the run-status domain are deliberately absent.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum FactoryRunSettledStatus {
    /** The {@code completed} variant. */
    COMPLETED("completed"),
    /** The {@code halted} variant. */
    HALTED("halted"),
    /** The {@code cancelled} variant. */
    CANCELLED("cancelled"),
    /** The {@code error} variant. */
    ERROR("error");

    private final String value;
    FactoryRunSettledStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FactoryRunSettledStatus fromValue(String value) {
        for (FactoryRunSettledStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FactoryRunSettledStatus value: " + value);
    }
}
