/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Derived lifecycle state of a factory phase.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum FactoryPhaseStatus {
    /** The {@code pending} variant. */
    PENDING("pending"),
    /** The {@code active} variant. */
    ACTIVE("active"),
    /** The {@code completed} variant. */
    COMPLETED("completed"),
    /** The {@code skipped} variant. */
    SKIPPED("skipped");

    private final String value;
    FactoryPhaseStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FactoryPhaseStatus fromValue(String value) {
        for (FactoryPhaseStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FactoryPhaseStatus value: " + value);
    }
}
