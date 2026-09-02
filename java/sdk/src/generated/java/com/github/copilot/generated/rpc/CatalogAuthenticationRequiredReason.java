/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Why the catalog authority did not accept the caller's identity
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum CatalogAuthenticationRequiredReason {
    /** The {@code no-credential} variant. */
    NO_CREDENTIAL("no-credential"),
    /** The {@code credential-expired} variant. */
    CREDENTIAL_EXPIRED("credential-expired"),
    /** The {@code credential-rejected} variant. */
    CREDENTIAL_REJECTED("credential-rejected");

    private final String value;
    CatalogAuthenticationRequiredReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CatalogAuthenticationRequiredReason fromValue(String value) {
        for (CatalogAuthenticationRequiredReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CatalogAuthenticationRequiredReason value: " + value);
    }
}
