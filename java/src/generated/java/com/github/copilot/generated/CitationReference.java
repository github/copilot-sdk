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
 * A single citation occurrence linking a span of generated text to a supporting source.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CitationReference(
    /** Identifier of the CitationSource this reference points to (CitationSource.id). */
    @JsonProperty("sourceId") String sourceId,
    /** The exact text from the source that supports the cited span, when provided by the model. */
    @JsonProperty("citedText") String citedText,
    /** Location within the source that supports the cited span, when the provider reports one. */
    @JsonProperty("location") Object location,
    /** Provider-native citation correlation data (e.g. Anthropic search_result_index / document_index), passed through opaquely for debugging and forward compatibility. */
    @JsonProperty("providerMetadata") Object providerMetadata
) {
}
