/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Task type determines the handoff strategy (CCA fetches events; CLI prepares a transient session).
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionsOpenHandoffTaskType {
    /** The {@code cca} variant. */
    CCA("cca"),
    /** The {@code cli} variant. */
    CLI("cli");

    private final String value;
    SessionsOpenHandoffTaskType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionsOpenHandoffTaskType fromValue(String value) {
        for (SessionsOpenHandoffTaskType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionsOpenHandoffTaskType value: " + value);
    }
}
