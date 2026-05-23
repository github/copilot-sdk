/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Whether this item is a queued user message or a queued slash command / model change
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum QueuePendingItemsKind {
    /** The {@code message} variant. */
    MESSAGE("message"),
    /** The {@code command} variant. */
    COMMAND("command");

    private final String value;
    QueuePendingItemsKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static QueuePendingItemsKind fromValue(String value) {
        for (QueuePendingItemsKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown QueuePendingItemsKind value: " + value);
    }
}
