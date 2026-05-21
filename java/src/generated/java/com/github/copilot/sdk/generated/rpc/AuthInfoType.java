/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Authentication type
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AuthInfoType {
    /** The {@code hmac} variant. */
    HMAC("hmac"),
    /** The {@code env} variant. */
    ENV("env"),
    /** The {@code user} variant. */
    USER("user"),
    /** The {@code gh-cli} variant. */
    GH_CLI("gh-cli"),
    /** The {@code api-key} variant. */
    API_KEY("api-key"),
    /** The {@code token} variant. */
    TOKEN("token"),
    /** The {@code copilot-api-token} variant. */
    COPILOT_API_TOKEN("copilot-api-token");

    private final String value;
    AuthInfoType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AuthInfoType fromValue(String value) {
        for (AuthInfoType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AuthInfoType value: " + value);
    }
}
