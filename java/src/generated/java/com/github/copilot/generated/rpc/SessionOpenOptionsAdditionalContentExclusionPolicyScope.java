/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Allowed values for the `SessionOpenOptionsAdditionalContentExclusionPolicyScope` enumeration.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionOpenOptionsAdditionalContentExclusionPolicyScope {
    /** The {@code repo} variant. */
    REPO("repo"),
    /** The {@code all} variant. */
    ALL("all");

    private final String value;
    SessionOpenOptionsAdditionalContentExclusionPolicyScope(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionOpenOptionsAdditionalContentExclusionPolicyScope fromValue(String value) {
        for (SessionOpenOptionsAdditionalContentExclusionPolicyScope v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionOpenOptionsAdditionalContentExclusionPolicyScope value: " + value);
    }
}
