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
 * A single host-driven completion. Accepting an item replaces `[rangeStart, rangeEnd)` (UTF-16 code units) in the composer with `insertText`; when the range is absent, the active token around the cursor is replaced.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionCompletionItem(
    /** Text spliced into the composer when the item is accepted. */
    @JsonProperty("insertText") String insertText,
    /** Start of the replacement range in `text`, in UTF-16 code units. */
    @JsonProperty("rangeStart") Long rangeStart,
    /** End (exclusive) of the replacement range in `text`, in UTF-16 code units. */
    @JsonProperty("rangeEnd") Long rangeEnd,
    /** Primary display label for the picker row. Falls back to `insertText` when absent. */
    @JsonProperty("label") String label,
    /** Render-kind hint for the picker row (e.g. `"document"`, `"directory"`), derived from the host's display kind. */
    @JsonProperty("kind") String kind
) {
}
