/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Terminal reason an Auto preference activation failed.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AutoTierSwitchFailureReason {
    /** The {@code policy_rejected} variant. */
    POLICY_REJECTED("policy_rejected"),
    /** The {@code request_failed} variant. */
    REQUEST_FAILED("request_failed"),
    /** The {@code setup_failed} variant. */
    SETUP_FAILED("setup_failed"),
    /** The {@code unsupported} variant. */
    UNSUPPORTED("unsupported");

    private final String value;
    AutoTierSwitchFailureReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AutoTierSwitchFailureReason fromValue(String value) {
        for (AutoTierSwitchFailureReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AutoTierSwitchFailureReason value: " + value);
    }
}
