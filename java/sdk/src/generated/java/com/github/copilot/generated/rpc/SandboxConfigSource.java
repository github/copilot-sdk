/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Origin of the sandbox choice supplied by the host. This value describes preference or session intent; it does not authorize bypassing managed policy.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SandboxConfigSource {
    /** The {@code never_configured} variant. */
    NEVER_CONFIGURED("never_configured"),
    /** The {@code user_enabled} variant. */
    USER_ENABLED("user_enabled"),
    /** The {@code user_disabled} variant. */
    USER_DISABLED("user_disabled"),
    /** The {@code session_flag} variant. */
    SESSION_FLAG("session_flag"),
    /** The {@code session_disabled} variant. */
    SESSION_DISABLED("session_disabled"),
    /** The {@code unsupported_host} variant. */
    UNSUPPORTED_HOST("unsupported_host"),
    /** The {@code repository_policy} variant. */
    REPOSITORY_POLICY("repository_policy");

    private final String value;
    SandboxConfigSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SandboxConfigSource fromValue(String value) {
        for (SandboxConfigSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SandboxConfigSource value: " + value);
    }
}
