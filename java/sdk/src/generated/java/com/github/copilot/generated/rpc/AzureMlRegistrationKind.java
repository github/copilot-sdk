/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Result category of a managed provider configuration change.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AzureMlRegistrationKind {
    /** The {@code written} variant. */
    WRITTEN("written"),
    /** The {@code skipped} variant. */
    SKIPPED("skipped"),
    /** The {@code failed} variant. */
    FAILED("failed");

    private final String value;
    AzureMlRegistrationKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AzureMlRegistrationKind fromValue(String value) {
        for (AzureMlRegistrationKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AzureMlRegistrationKind value: " + value);
    }
}
