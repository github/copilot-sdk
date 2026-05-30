/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Type of GitHub reference
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SendAttachmentGithubReferenceType {
    /** The {@code issue} variant. */
    ISSUE("issue"),
    /** The {@code pr} variant. */
    PR("pr"),
    /** The {@code discussion} variant. */
    DISCUSSION("discussion");

    private final String value;
    SendAttachmentGithubReferenceType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SendAttachmentGithubReferenceType fromValue(String value) {
        for (SendAttachmentGithubReferenceType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SendAttachmentGithubReferenceType value: " + value);
    }
}
