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
 * The currently selected model, reasoning effort, and context tier for the session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelGetCurrentResult(
    /** Currently active model identifier */
    @JsonProperty("modelId") String modelId,
    /** Reasoning effort level currently applied to the active model, when one is set. Reads `Session.getReasoningEffort()` synchronously after `getSelectedModel()` resolves so the two values are reported as a snapshot. */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** Context tier currently pinned for the session, when one is set. Reflects `Session.getContextTier()`, restored from the session journal on resume. */
    @JsonProperty("contextTier") ContextTier contextTier
) {
}
