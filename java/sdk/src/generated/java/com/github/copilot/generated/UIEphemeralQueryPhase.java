/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Lifecycle phase for a Rust-owned ephemeral query stream.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum UIEphemeralQueryPhase {
    /** The {@code started} variant. */
    STARTED("started"),
    /** The {@code chunk} variant. */
    CHUNK("chunk"),
    /** The {@code completed} variant. */
    COMPLETED("completed"),
    /** The {@code failed} variant. */
    FAILED("failed"),
    /** The {@code aborted} variant. */
    ABORTED("aborted");

    private final String value;
    UIEphemeralQueryPhase(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static UIEphemeralQueryPhase fromValue(String value) {
        for (UIEphemeralQueryPhase v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown UIEphemeralQueryPhase value: " + value);
    }
}
