/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * The user action: "accept" (submitted form), "decline" (explicitly refused), or "cancel" (dismissed)
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ElicitationCompletedAction {
    /** The {@code accept} variant. */
    ACCEPT("accept"),
    /** The {@code decline} variant. */
    DECLINE("decline"),
    /** The {@code cancel} variant. */
    CANCEL("cancel");

    private final String value;
    ElicitationCompletedAction(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ElicitationCompletedAction fromValue(String value) {
        for (ElicitationCompletedAction v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ElicitationCompletedAction value: " + value);
    }
}
