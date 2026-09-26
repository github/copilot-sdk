/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * One captured source at this placement and observation boundary.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkerSource(
    /** Indivisible accepted input identity. */
    @JsonProperty("input") WorkerInput input,
    /** Actual admissions in capture order; at most 32. */
    @JsonProperty("admissions") List<WorkerAdmission> admissions,
    /** Applicable observations were all captured here, never work completion or success. */
    @JsonProperty("captureComplete") Boolean captureComplete,
    /** Actual completion emission only. Absence is not an execution outcome. */
    @JsonProperty("completion") WorkerEventReference completion,
    /** Actual consumed notification, when observed. */
    @JsonProperty("notification") WorkerNotificationReference notification,
    /** Exact already-open iteration when an immediate notification was consumed. */
    @JsonProperty("admittedDuring") WorkerEventReference admittedDuring
) {
}
