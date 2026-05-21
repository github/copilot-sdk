/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadata for session lifecycle events.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionLifecycleEventMetadata(@JsonProperty("startTime") String startTime,
        @JsonProperty("modifiedTime") String modifiedTime, @JsonProperty("summary") String summary) {
}
