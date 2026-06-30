/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * User action selected for an exhausted session limit.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum UISessionLimitsExhaustedResponseAction {
    /** The {@code add} variant. */
    ADD("add"),
    /** The {@code set} variant. */
    SET("set"),
    /** The {@code unset} variant. */
    UNSET("unset"),
    /** The {@code cancel} variant. */
    CANCEL("cancel");

    private final String value;
    UISessionLimitsExhaustedResponseAction(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static UISessionLimitsExhaustedResponseAction fromValue(String value) {
        for (UISessionLimitsExhaustedResponseAction v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown UISessionLimitsExhaustedResponseAction value: " + value);
    }
}
