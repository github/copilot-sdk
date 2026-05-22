/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import javax.annotation.processing.Generated;

/**
 * The outcome of the permission request
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum PermissionCompletedKind {
    /** The {@code approved} variant. */
    APPROVED("approved"),
    /** The {@code approved-for-session} variant. */
    APPROVED_FOR_SESSION("approved-for-session"),
    /** The {@code approved-for-location} variant. */
    APPROVED_FOR_LOCATION("approved-for-location"),
    /** The {@code denied-by-rules} variant. */
    DENIED_BY_RULES("denied-by-rules"),
    /** The {@code denied-no-approval-rule-and-could-not-request-from-user} variant. */
    DENIED_NO_APPROVAL_RULE_AND_COULD_NOT_REQUEST_FROM_USER("denied-no-approval-rule-and-could-not-request-from-user"),
    /** The {@code denied-interactively-by-user} variant. */
    DENIED_INTERACTIVELY_BY_USER("denied-interactively-by-user"),
    /** The {@code denied-by-content-exclusion-policy} variant. */
    DENIED_BY_CONTENT_EXCLUSION_POLICY("denied-by-content-exclusion-policy"),
    /** The {@code denied-by-permission-request-hook} variant. */
    DENIED_BY_PERMISSION_REQUEST_HOOK("denied-by-permission-request-hook");

    private final String value;
    PermissionCompletedKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static PermissionCompletedKind fromValue(String value) {
        for (PermissionCompletedKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown PermissionCompletedKind value: " + value);
    }
}
