/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Credential availability, not provider authentication or health.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SavedProviderCredentialStatus {
    /** The {@code present} variant. */
    PRESENT("present"),
    /** The {@code absent} variant. */
    ABSENT("absent"),
    /** The {@code unavailable} variant. */
    UNAVAILABLE("unavailable");

    private final String value;
    SavedProviderCredentialStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SavedProviderCredentialStatus fromValue(String value) {
        for (SavedProviderCredentialStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SavedProviderCredentialStatus value: " + value);
    }
}
