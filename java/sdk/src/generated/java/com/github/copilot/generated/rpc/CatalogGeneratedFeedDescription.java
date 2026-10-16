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
 * A generated feed's identity. It has no version; generatedAt is its only freshness signal.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogGeneratedFeedDescription(
    /** Generation strategy that was requested. */
    @JsonProperty("strategy") String strategy,
    /** Feed key that was requested. */
    @JsonProperty("key") String key,
    /** When the feed was computed, as reported by the authority. Omitted when the authority reports no valid timestamp. */
    @JsonProperty("generatedAt") String generatedAt
) {
}
