/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Type of GitHub reference
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum UserMessageAttachmentGitHubReferenceType {
    /** The {@code issue} variant. */
    ISSUE("issue"),
    /** The {@code pr} variant. */
    PR("pr"),
    /** The {@code discussion} variant. */
    DISCUSSION("discussion");

    private final String value;
    UserMessageAttachmentGitHubReferenceType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static UserMessageAttachmentGitHubReferenceType fromValue(String value) {
        for (UserMessageAttachmentGitHubReferenceType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown UserMessageAttachmentGitHubReferenceType value: " + value);
    }
}
