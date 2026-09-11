/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Why the runtime requests client-task cancellation.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ClientTaskCancelReason {
    /** The {@code cancel_requested} variant. */
    CANCEL_REQUESTED("cancel_requested"),
    /** The {@code session_shutdown} variant. */
    SESSION_SHUTDOWN("session_shutdown");

    private final String value;
    ClientTaskCancelReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ClientTaskCancelReason fromValue(String value) {
        for (ClientTaskCancelReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ClientTaskCancelReason value: " + value);
    }
}
