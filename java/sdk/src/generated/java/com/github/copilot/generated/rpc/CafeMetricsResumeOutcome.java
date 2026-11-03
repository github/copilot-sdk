/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Terminal resume outcome.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum CafeMetricsResumeOutcome {
    /** The {@code success} variant. */
    SUCCESS("success"),
    /** The {@code recovered} variant. */
    RECOVERED("recovered"),
    /** The {@code failed} variant. */
    FAILED("failed"),
    /** The {@code not_found} variant. */
    NOT_FOUND("not_found");

    private final String value;
    CafeMetricsResumeOutcome(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CafeMetricsResumeOutcome fromValue(String value) {
        for (CafeMetricsResumeOutcome v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CafeMetricsResumeOutcome value: " + value);
    }
}
