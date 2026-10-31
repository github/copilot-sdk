/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Outcome of applying the session-idle Autopilot continuation policy.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum AutopilotEvaluateIdleContinuationOutcome {
    /** The {@code continued} variant. */
    CONTINUED("continued"),
    /** The {@code stopped} variant. */
    STOPPED("stopped"),
    /** The {@code notApplicable} variant. */
    NOTAPPLICABLE("notApplicable");

    private final String value;
    AutopilotEvaluateIdleContinuationOutcome(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AutopilotEvaluateIdleContinuationOutcome fromValue(String value) {
        for (AutopilotEvaluateIdleContinuationOutcome v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AutopilotEvaluateIdleContinuationOutcome value: " + value);
    }
}
