/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Interaction policy for host-owned Microsoft Entra acquisition.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum EntraTokenAcquireReason {
    /** The {@code signIn} variant. */
    SIGNIN("signIn"),
    /** The {@code renewal} variant. */
    RENEWAL("renewal"),
    /** The {@code forceInteractive} variant. */
    FORCEINTERACTIVE("forceInteractive");

    private final String value;
    EntraTokenAcquireReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static EntraTokenAcquireReason fromValue(String value) {
        for (EntraTokenAcquireReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown EntraTokenAcquireReason value: " + value);
    }
}
