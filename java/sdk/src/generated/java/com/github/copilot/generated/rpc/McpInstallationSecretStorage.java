/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Explicit backend selection is part of the final review; failures never switch backends.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpInstallationSecretStorage {
    /** The {@code keychain} variant. */
    KEYCHAIN("keychain"),
    /** The {@code private-file} variant. */
    PRIVATE_FILE("private-file");

    private final String value;
    McpInstallationSecretStorage(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpInstallationSecretStorage fromValue(String value) {
        for (McpInstallationSecretStorage v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpInstallationSecretStorage value: " + value);
    }
}
