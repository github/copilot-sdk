/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Reason a rewind read (rewind points, file-restore preview, or session diff) could not be answered from the session's file-change captures.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum HistoryRewindUnavailableReason {
    /** The {@code file-change-tracking-disabled} variant. */
    FILE_CHANGE_TRACKING_DISABLED("file-change-tracking-disabled"),
    /** The {@code session-busy} variant. */
    SESSION_BUSY("session-busy"),
    /** The {@code unsupported-remote-session} variant. */
    UNSUPPORTED_REMOTE_SESSION("unsupported-remote-session");

    private final String value;
    HistoryRewindUnavailableReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static HistoryRewindUnavailableReason fromValue(String value) {
        for (HistoryRewindUnavailableReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown HistoryRewindUnavailableReason value: " + value);
    }
}
