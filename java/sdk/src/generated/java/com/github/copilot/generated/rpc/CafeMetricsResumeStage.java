/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Last resume phase.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum CafeMetricsResumeStage {
    /** The {@code event_log} variant. */
    EVENT_LOG("event_log"),
    /** The {@code construction} variant. */
    CONSTRUCTION("construction"),
    /** The {@code queue_restore} variant. */
    QUEUE_RESTORE("queue_restore"),
    /** The {@code complete} variant. */
    COMPLETE("complete");

    private final String value;
    CafeMetricsResumeStage(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CafeMetricsResumeStage fromValue(String value) {
        for (CafeMetricsResumeStage v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CafeMetricsResumeStage value: " + value);
    }
}
