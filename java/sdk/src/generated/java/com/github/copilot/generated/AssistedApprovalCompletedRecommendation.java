/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Usable verdict returned by the Assisted Permissions judge.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AssistedApprovalCompletedRecommendation {
    /** The {@code approve} variant. */
    APPROVE("approve"),
    /** The {@code requireApproval} variant. */
    REQUIREAPPROVAL("requireApproval");

    private final String value;
    AssistedApprovalCompletedRecommendation(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AssistedApprovalCompletedRecommendation fromValue(String value) {
        for (AssistedApprovalCompletedRecommendation v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AssistedApprovalCompletedRecommendation value: " + value);
    }
}
