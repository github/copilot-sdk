/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * How the person refused a built-in `ask_user` question instead of answering it.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum HumanResponseAskUserAction {
    /** The {@code decline} variant. */
    DECLINE("decline"),
    /** The {@code cancel} variant. */
    CANCEL("cancel");

    private final String value;
    HumanResponseAskUserAction(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static HumanResponseAskUserAction fromValue(String value) {
        for (HumanResponseAskUserAction v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown HumanResponseAskUserAction value: " + value);
    }
}
