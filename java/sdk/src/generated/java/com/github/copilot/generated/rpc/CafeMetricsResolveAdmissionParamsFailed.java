/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Variant {@code failed} of {@link CafeMetricsResolveAdmissionParams}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class CafeMetricsResolveAdmissionParamsFailed extends CafeMetricsResolveAdmissionParams {

    @JsonProperty("kind")
    private final String kind = "failed";

    @Override
    public String getKind() { return kind; }

    /** Finite non-negative lookup duration in milliseconds. */
    @JsonProperty("lookupDurationMs")
    private Double lookupDurationMs;

    public Double getLookupDurationMs() { return lookupDurationMs; }
    public void setLookupDurationMs(Double lookupDurationMs) { this.lookupDurationMs = lookupDurationMs; }
}
