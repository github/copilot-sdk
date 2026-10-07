/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Terminal disposition of a login persistence attempt.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AuthLoginResultStatus {
    /** The {@code completed} variant. */
    COMPLETED("completed"),
    /** The {@code needs-plaintext-consent} variant. */
    NEEDS_PLAINTEXT_CONSENT("needs-plaintext-consent"),
    /** The {@code declined} variant. */
    DECLINED("declined");

    private final String value;
    AuthLoginResultStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AuthLoginResultStatus fromValue(String value) {
        for (AuthLoginResultStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AuthLoginResultStatus value: " + value);
    }
}
