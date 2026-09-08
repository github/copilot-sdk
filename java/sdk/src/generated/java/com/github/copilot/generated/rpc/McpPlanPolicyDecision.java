/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * What policy decided for a planned server
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpPlanPolicyDecision {
    /** The {@code allowed} variant. */
    ALLOWED("allowed"),
    /** The {@code blocked} variant. */
    BLOCKED("blocked"),
    /** The {@code requires-approval} variant. */
    REQUIRES_APPROVAL("requires-approval");

    private final String value;
    McpPlanPolicyDecision(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpPlanPolicyDecision fromValue(String value) {
        for (McpPlanPolicyDecision v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpPlanPolicyDecision value: " + value);
    }
}
