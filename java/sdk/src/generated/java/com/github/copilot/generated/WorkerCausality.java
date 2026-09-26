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
 * Optional v1 worker diagnostics. The compact UTF-8 {"workerCausality":value}
must fit 4096 bytes. Ignore invalid/unknown/oversize metadata, not the product event.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkerCausality(
    /** Supported version, exactly 1. */
    @JsonProperty("version") Long version,
    /** Provenance of this enclosing observation; never inferred from other fields. */
    @JsonProperty("observationProvenance") WorkerObservationProvenance observationProvenance,
    /** Sources in capture order, at most 32. Absent/unknown is not known-empty. */
    @JsonProperty("sources") List<WorkerSource> sources,
    /** Complete placement-aware observed capture, not global causality or execution success.
Empty true requires explicit native invocation attestation. */
    @JsonProperty("captureComplete") Boolean captureComplete
) {
}
