/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Cursor status: 'ok' means the read succeeded against the requested history; 'expired' means the requested continuation is unavailable. Recovery is endpoint-specific: session.eventLog.read returns a boundary window of remaining active history that may overlap prior pages, while sessions.readPersistedEvents returns an empty terminal page and never switches journal generations. An expired persisted read is not successful completion; a complete persisted snapshot requires cursorStatus 'ok' and hasMore false.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum EventsCursorStatus {
    /** The {@code ok} variant. */
    OK("ok"),
    /** The {@code expired} variant. */
    EXPIRED("expired");

    private final String value;
    EventsCursorStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static EventsCursorStatus fromValue(String value) {
        for (EventsCursorStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown EventsCursorStatus value: " + value);
    }
}
