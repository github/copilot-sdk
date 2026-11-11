/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Stable credential-context failure categories, independent of the credential provider.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CredentialContextErrorCode {
    /** The {@code credential_unavailable} variant. */
    CREDENTIAL_UNAVAILABLE("credential_unavailable"),
    /** The {@code credential_revoked} variant. */
    CREDENTIAL_REVOKED("credential_revoked"),
    /** The {@code credential_denied} variant. */
    CREDENTIAL_DENIED("credential_denied"),
    /** The {@code credential_resolution_failed} variant. */
    CREDENTIAL_RESOLUTION_FAILED("credential_resolution_failed");

    private final String value;
    CredentialContextErrorCode(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CredentialContextErrorCode fromValue(String value) {
        for (CredentialContextErrorCode v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CredentialContextErrorCode value: " + value);
    }
}
