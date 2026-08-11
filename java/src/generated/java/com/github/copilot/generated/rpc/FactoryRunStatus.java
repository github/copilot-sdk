/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Current or terminal state of a factory run.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum FactoryRunStatus {
    /** The {@code pending} variant. */
    PENDING("pending"),
    /** The {@code running} variant. */
    RUNNING("running"),
    /** The {@code completed} variant. */
    COMPLETED("completed"),
    /** The {@code halted} variant. */
    HALTED("halted"),
    /** The {@code cancelled} variant. */
    CANCELLED("cancelled"),
    /** The {@code error} variant. */
    ERROR("error");

    private final String value;
    FactoryRunStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FactoryRunStatus fromValue(String value) {
        for (FactoryRunStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FactoryRunStatus value: " + value);
    }
}
