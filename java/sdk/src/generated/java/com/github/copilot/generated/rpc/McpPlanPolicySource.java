/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Which authority produced a policy decision
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpPlanPolicySource {
    /** The {@code none} variant. */
    NONE("none"),
    /** The {@code enterprise-allowlist} variant. */
    ENTERPRISE_ALLOWLIST("enterprise-allowlist"),
    /** The {@code registry-policy} variant. */
    REGISTRY_POLICY("registry-policy"),
    /** The {@code local-trust} variant. */
    LOCAL_TRUST("local-trust");

    private final String value;
    McpPlanPolicySource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpPlanPolicySource fromValue(String value) {
        for (McpPlanPolicySource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpPlanPolicySource value: " + value);
    }
}
