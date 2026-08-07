/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Controlled reason or actor responsible for a permission response.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionDecisionSource {
    /** The {@code judge_recommendation} variant. */
    JUDGE_RECOMMENDATION("judge_recommendation"),
    /** The {@code human_response} variant. */
    HUMAN_RESPONSE("human_response"),
    /** The {@code host_policy} variant. */
    HOST_POLICY("host_policy"),
    /** The {@code unattended_fallback} variant. */
    UNATTENDED_FALLBACK("unattended_fallback");

    private final String value;
    PermissionDecisionSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionDecisionSource fromValue(String value) {
        for (PermissionDecisionSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionDecisionSource value: " + value);
    }
}
