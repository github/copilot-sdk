/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Current policy state for this model
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelPolicyState {
    /** The {@code enabled} variant. */
    ENABLED("enabled"),
    /** The {@code disabled} variant. */
    DISABLED("disabled"),
    /** The {@code unconfigured} variant. */
    UNCONFIGURED("unconfigured");

    private final String value;
    ModelPolicyState(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelPolicyState fromValue(String value) {
        for (ModelPolicyState v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelPolicyState value: " + value);
    }
}
