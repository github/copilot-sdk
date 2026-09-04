/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * What the user must do to recover from a failure, named as an action rather than as one client's affordance. The runtime cannot know which affordance a client offers — a slash command, a settings pane, a link — so the accompanying message stays host-agnostic and each client renders its own copy from this value. Absent when the runtime knows of no action the user can take.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum RemediationAction {
    /** The {@code sign_in} variant. */
    SIGN_IN("sign_in"),
    /** The {@code switch_account} variant. */
    SWITCH_ACCOUNT("switch_account"),
    /** The {@code show_account} variant. */
    SHOW_ACCOUNT("show_account"),
    /** The {@code review_sandbox_policy} variant. */
    REVIEW_SANDBOX_POLICY("review_sandbox_policy"),
    /** The {@code allow_sandbox_outbound} variant. */
    ALLOW_SANDBOX_OUTBOUND("allow_sandbox_outbound");

    private final String value;
    RemediationAction(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static RemediationAction fromValue(String value) {
        for (RemediationAction v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown RemediationAction value: " + value);
    }
}
