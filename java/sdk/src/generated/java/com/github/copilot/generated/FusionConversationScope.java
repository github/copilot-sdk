/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Conversation scope in which a HydraFusion phase executes.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum FusionConversationScope {
    /** The {@code root} variant. */
    ROOT("root"),
    /** The {@code review} variant. */
    REVIEW("review");

    private final String value;
    FusionConversationScope(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FusionConversationScope fromValue(String value) {
        for (FusionConversationScope v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown FusionConversationScope value: " + value);
    }
}
