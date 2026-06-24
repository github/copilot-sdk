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
 * A contiguous span of generated assistant text and the source references that support it.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CitationSpan(
    /** Start offset of the cited span within the final assistant message content (UTF-16 code units, zero-based, inclusive). */
    @JsonProperty("startIndex") Long startIndex,
    /** End offset of the cited span within the final assistant message content (UTF-16 code units, zero-based, exclusive). */
    @JsonProperty("endIndex") Long endIndex,
    /** The sources that support this span of generated text. */
    @JsonProperty("references") List<CitationReference> references
) {
}
