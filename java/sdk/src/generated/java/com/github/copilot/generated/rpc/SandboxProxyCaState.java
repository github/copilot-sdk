/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * State of the persistent certificate authority of the sandbox credential proxy.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SandboxProxyCaState {
    /** The {@code unsupported} variant. */
    UNSUPPORTED("unsupported"),
    /** The {@code notInstalled} variant. */
    NOTINSTALLED("notInstalled"),
    /** The {@code installed} variant. */
    INSTALLED("installed"),
    /** The {@code error} variant. */
    ERROR("error");

    private final String value;
    SandboxProxyCaState(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SandboxProxyCaState fromValue(String value) {
        for (SandboxProxyCaState v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SandboxProxyCaState value: " + value);
    }
}
