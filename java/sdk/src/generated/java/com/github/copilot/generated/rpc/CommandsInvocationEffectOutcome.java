/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Whether a pending slash-command invocation effect was applied or cancelled by the host.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CommandsInvocationEffectOutcome {
    /** The {@code applied} variant. */
    APPLIED("applied"),
    /** The {@code cancelled} variant. */
    CANCELLED("cancelled");

    private final String value;
    CommandsInvocationEffectOutcome(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CommandsInvocationEffectOutcome fromValue(String value) {
        for (CommandsInvocationEffectOutcome v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CommandsInvocationEffectOutcome value: " + value);
    }
}
