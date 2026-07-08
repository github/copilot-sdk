/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * User action selected for an exhausted session limit.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionLimitsExhaustedResponseAction {
    /** The {@code add} variant. */
    ADD("add"),
    /** The {@code set} variant. */
    SET("set"),
    /** The {@code unset} variant. */
    UNSET("unset"),
    /** The {@code cancel} variant. */
    CANCEL("cancel");

    private final String value;
    SessionLimitsExhaustedResponseAction(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionLimitsExhaustedResponseAction fromValue(String value) {
        for (SessionLimitsExhaustedResponseAction v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionLimitsExhaustedResponseAction value: " + value);
    }
}
