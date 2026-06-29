/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * How this user message was delivered to the agentic loop, relative to whether the loop was already running. This is the timing axis only; the message's origin (human vs. system/command/schedule/skill/etc.) is carried separately by `source`. A system-injected message has a delivery too — e.g. a background-task notification waking an idle agent is `idle`, the same mechanism as a human starting a fresh turn.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum UserMessageDelivery {
    /** The {@code idle} variant. */
    IDLE("idle"),
    /** The {@code steering} variant. */
    STEERING("steering"),
    /** The {@code queued} variant. */
    QUEUED("queued");

    private final String value;
    UserMessageDelivery(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static UserMessageDelivery fromValue(String value) {
        for (UserMessageDelivery v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown UserMessageDelivery value: " + value);
    }
}
