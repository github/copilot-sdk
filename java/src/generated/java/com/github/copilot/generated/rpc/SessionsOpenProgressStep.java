/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Handoff step.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionsOpenProgressStep {
    /** The {@code load-session} variant. */
    LOAD_SESSION("load-session"),
    /** The {@code validate-repo} variant. */
    VALIDATE_REPO("validate-repo"),
    /** The {@code check-changes} variant. */
    CHECK_CHANGES("check-changes"),
    /** The {@code checkout-branch} variant. */
    CHECKOUT_BRANCH("checkout-branch"),
    /** The {@code create-session} variant. */
    CREATE_SESSION("create-session"),
    /** The {@code save-session} variant. */
    SAVE_SESSION("save-session");

    private final String value;
    SessionsOpenProgressStep(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionsOpenProgressStep fromValue(String value) {
        for (SessionsOpenProgressStep v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionsOpenProgressStep value: " + value);
    }
}
