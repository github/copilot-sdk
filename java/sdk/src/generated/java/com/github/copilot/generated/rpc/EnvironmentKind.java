/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * GitHub Mission Control compute kind.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum EnvironmentKind {
    /** The {@code user-local} variant. */
    USER_LOCAL("user-local"),
    /** The {@code user-codespace} variant. */
    USER_CODESPACE("user-codespace"),
    /** The {@code managed-actions} variant. */
    MANAGED_ACTIONS("managed-actions"),
    /** The {@code managed-sandbox} variant. */
    MANAGED_SANDBOX("managed-sandbox"),
    /** The {@code managed-cca} variant. */
    MANAGED_CCA("managed-cca");

    private final String value;
    EnvironmentKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static EnvironmentKind fromValue(String value) {
        for (EnvironmentKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown EnvironmentKind value: " + value);
    }
}
