/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Where and when the runtime observed the trust metadata. Observation time is not the authority's evaluation time and must not be used to infer staleness.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogTrustProvenance(
    /** Bounded authority that supplied the trust field. */
    @JsonProperty("source") CatalogTrustSource source,
    /** ISO 8601 timestamp with a timezone offset at which the runtime observed the search result carrying this trust field. */
    @JsonProperty("observedAt") OffsetDateTime observedAt
) {
}
