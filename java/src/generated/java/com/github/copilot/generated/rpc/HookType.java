/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Hook event name dispatched through the SDK callback transport.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum HookType {
    /** The {@code preToolUse} variant. */
    PRETOOLUSE("preToolUse"),
    /** The {@code preMcpToolCall} variant. */
    PREMCPTOOLCALL("preMcpToolCall"),
    /** The {@code postToolUse} variant. */
    POSTTOOLUSE("postToolUse"),
    /** The {@code postToolUseFailure} variant. */
    POSTTOOLUSEFAILURE("postToolUseFailure"),
    /** The {@code userPromptSubmitted} variant. */
    USERPROMPTSUBMITTED("userPromptSubmitted"),
    /** The {@code userPromptTransformed} variant. */
    USERPROMPTTRANSFORMED("userPromptTransformed"),
    /** The {@code sessionStart} variant. */
    SESSIONSTART("sessionStart"),
    /** The {@code sessionEnd} variant. */
    SESSIONEND("sessionEnd"),
    /** The {@code postResult} variant. */
    POSTRESULT("postResult"),
    /** The {@code prePRDescription} variant. */
    PREPRDESCRIPTION("prePRDescription"),
    /** The {@code errorOccurred} variant. */
    ERROROCCURRED("errorOccurred"),
    /** The {@code agentStop} variant. */
    AGENTSTOP("agentStop"),
    /** The {@code subagentStart} variant. */
    SUBAGENTSTART("subagentStart"),
    /** The {@code subagentStop} variant. */
    SUBAGENTSTOP("subagentStop"),
    /** The {@code preCompact} variant. */
    PRECOMPACT("preCompact"),
    /** The {@code permissionRequest} variant. */
    PERMISSIONREQUEST("permissionRequest"),
    /** The {@code notification} variant. */
    NOTIFICATION("notification");

    private final String value;
    HookType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static HookType fromValue(String value) {
        for (HookType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown HookType value: " + value);
    }
}
