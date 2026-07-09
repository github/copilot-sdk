/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Sharing status for a synced session. "repo" makes the session visible to anyone with read access to the repository; "unshared" restricts it to the creator and collaborators.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionVisibilityStatus {
    /** The {@code repo} variant. */
    REPO("repo"),
    /** The {@code unshared} variant. */
    UNSHARED("unshared");

    private final String value;
    SessionVisibilityStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionVisibilityStatus fromValue(String value) {
        for (SessionVisibilityStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionVisibilityStatus value: " + value);
    }
}
