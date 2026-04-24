/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum UIElicitationResponseAction {
    /** The {@code accept} variant. */
    ACCEPT("accept"),
    /** The {@code decline} variant. */
    DECLINE("decline"),
    /** The {@code cancel} variant. */
    CANCEL("cancel");

    private final String value;
    UIElicitationResponseAction(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static UIElicitationResponseAction fromValue(String value) {
        for (UIElicitationResponseAction v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown UIElicitationResponseAction value: " + value);
    }
}
