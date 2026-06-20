/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * A source that backs one or more cited spans in the assistant's response.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CitationSource(
    /** Stable, turn-scoped identifier for this source, referenced by CitationReference.sourceId. */
    @JsonProperty("id") String id,
    /** The system that produced this citation. */
    @JsonProperty("provider") CitationProvider provider,
    /** Human-readable title of the source. */
    @JsonProperty("title") String title,
    /** URL of the source, when it is a web resource. */
    @JsonProperty("url") String url,
    /** File path relative to the agent's workspace root, when the source is a file. */
    @JsonProperty("path") String path
) {
}
