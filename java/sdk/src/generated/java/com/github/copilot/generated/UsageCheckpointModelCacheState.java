/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Internal prompt-cache expiration state for one model
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageCheckpointModelCacheState(
    /** Model identifier associated with this cache state */
    @JsonProperty("modelId") String modelId,
    /** Latest known prompt-cache expiration */
    @JsonProperty("cacheExpiresAt") OffsetDateTime cacheExpiresAt,
    /** Retained cache lifetime in seconds, used to refresh expiration after a cache read */
    @JsonProperty("cacheTtlSeconds") Long cacheTtlSeconds
) {
}
