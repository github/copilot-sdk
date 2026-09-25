/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * The provider kind stamped on a signed-in account.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AccountKind {
    /** The {@code githubDotCom} variant. */
    GITHUBDOTCOM("githubDotCom"),
    /** The {@code proxima} variant. */
    PROXIMA("proxima"),
    /** The {@code entraEmu} variant. */
    ENTRAEMU("entraEmu"),
    /** The {@code entra} variant. */
    ENTRA("entra"),
    /** The {@code loki} variant. */
    LOKI("loki");

    private final String value;
    AccountKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AccountKind fromValue(String value) {
        for (AccountKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AccountKind value: " + value);
    }
}
