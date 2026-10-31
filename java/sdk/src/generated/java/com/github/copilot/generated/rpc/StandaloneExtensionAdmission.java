/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Runtime-derived standalone extension eligibility.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum StandaloneExtensionAdmission {
    /** The {@code allowed} variant. */
    ALLOWED("allowed"),
    /** The {@code plugin_only} variant. */
    PLUGIN_ONLY("plugin_only"),
    /** The {@code pending} variant. */
    PENDING("pending");

    private final String value;
    StandaloneExtensionAdmission(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static StandaloneExtensionAdmission fromValue(String value) {
        for (StandaloneExtensionAdmission v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown StandaloneExtensionAdmission value: " + value);
    }
}
