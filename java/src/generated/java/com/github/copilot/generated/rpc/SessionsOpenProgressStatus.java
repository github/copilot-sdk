/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Step status.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionsOpenProgressStatus {
    /** The {@code in-progress} variant. */
    IN_PROGRESS("in-progress"),
    /** The {@code complete} variant. */
    COMPLETE("complete");

    private final String value;
    SessionsOpenProgressStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionsOpenProgressStatus fromValue(String value) {
        for (SessionsOpenProgressStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionsOpenProgressStatus value: " + value);
    }
}
