/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Cursor status: 'ok' means the cursor was applied successfully; 'expired' means the cursor referred to an event that no longer exists in history (e.g. truncated or compacted away) and the read fell back to a boundary of the remaining history (the beginning for a forward read, the tail for a backward read). The fallback page is a fresh boundary snapshot, not a continuation of the requested cursor, so it may overlap already-rendered events; on 'expired' a consumer should reset/rebase its pagination state (or deduplicate by event id) before continuing from the returned cursor.
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
