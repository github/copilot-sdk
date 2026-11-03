/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Bounded resume reason, without error text.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum CafeMetricsResumeReason {
    /** The {@code none} variant. */
    NONE("none"),
    /** The {@code log_recovered} variant. */
    LOG_RECOVERED("log_recovered"),
    /** The {@code log_corrupt} variant. */
    LOG_CORRUPT("log_corrupt"),
    /** The {@code legacy_format} variant. */
    LEGACY_FORMAT("legacy_format"),
    /** The {@code recovery_io} variant. */
    RECOVERY_IO("recovery_io"),
    /** The {@code hydration} variant. */
    HYDRATION("hydration"),
    /** The {@code construction} variant. */
    CONSTRUCTION("construction"),
    /** The {@code queue} variant. */
    QUEUE("queue"),
    /** The {@code not_found} variant. */
    NOT_FOUND("not_found");

    private final String value;
    CafeMetricsResumeReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CafeMetricsResumeReason fromValue(String value) {
        for (CafeMetricsResumeReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown CafeMetricsResumeReason value: " + value);
    }
}
