/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * A provider a consumer may interactively sign in with.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum LoginProviderKind {
    /** The {@code githubDotCom} variant. */
    GITHUBDOTCOM("githubDotCom"),
    /** The {@code proxima} variant. */
    PROXIMA("proxima"),
    /** The {@code entra} variant. */
    ENTRA("entra");

    private final String value;
    LoginProviderKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static LoginProviderKind fromValue(String value) {
        for (LoginProviderKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown LoginProviderKind value: " + value);
    }
}
