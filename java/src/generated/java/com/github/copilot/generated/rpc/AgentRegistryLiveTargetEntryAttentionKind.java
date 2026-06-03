/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Kind of attention required when status === "attention". Meaningful only when status === "attention".
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AgentRegistryLiveTargetEntryAttentionKind {
    /** The {@code error} variant. */
    ERROR("error"),
    /** The {@code permission} variant. */
    PERMISSION("permission"),
    /** The {@code exit_plan} variant. */
    EXIT_PLAN("exit_plan"),
    /** The {@code elicitation} variant. */
    ELICITATION("elicitation"),
    /** The {@code user_input} variant. */
    USER_INPUT("user_input");

    private final String value;
    AgentRegistryLiveTargetEntryAttentionKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AgentRegistryLiveTargetEntryAttentionKind fromValue(String value) {
        for (AgentRegistryLiveTargetEntryAttentionKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AgentRegistryLiveTargetEntryAttentionKind value: " + value);
    }
}
