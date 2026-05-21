/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * Finite reason code describing why the current turn was aborted
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AbortReason {
    /** The {@code user_initiated} variant. */
    USER_INITIATED("user_initiated"),
    /** The {@code remote_command} variant. */
    REMOTE_COMMAND("remote_command"),
    /** The {@code user_abort} variant. */
    USER_ABORT("user_abort");

    private final String value;
    AbortReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AbortReason fromValue(String value) {
        for (AbortReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AbortReason value: " + value);
    }
}
