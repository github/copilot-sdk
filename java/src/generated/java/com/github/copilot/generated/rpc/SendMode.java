/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * How to deliver the message. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SendMode {
    /** The {@code enqueue} variant. */
    ENQUEUE("enqueue"),
    /** The {@code immediate} variant. */
    IMMEDIATE("immediate");

    private final String value;
    SendMode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SendMode fromValue(String value) {
        for (SendMode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SendMode value: " + value);
    }
}
