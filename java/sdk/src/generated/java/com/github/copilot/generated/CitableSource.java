/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * A source supplied by a tool that should be made available to the model as citable content.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CitableSource(
    /** Stable identifier for this source within the tool result. Used for deduplication and may be used by future provider integrations to correlate response citations back to the originating source. */
    @JsonProperty("id") String id,
    /** Human-readable title of the source. */
    @JsonProperty("title") String title,
    /** The source text made available to the model as citable content. */
    @JsonProperty("content") String content,
    /** URL of the source, when it is a web resource. */
    @JsonProperty("url") String url,
    /** File path relative to the agent's workspace root, when the source is a file. */
    @JsonProperty("path") String path
) {
}
