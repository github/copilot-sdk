/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * The session's authoritative model snapshot. Auto preference fields are configuration for the virtual `auto` model and do not change the selected model identifier. The context tier reflects `Session.getContextTier()`, restored from the session journal on resume.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelGetCurrentResult(
    /** Currently active model identifier */
    @JsonProperty("modelId") String modelId,
    /** Reasoning effort level currently applied to the active model, when one is set. Reads `Session.getReasoningEffort()` synchronously after `getSelectedModel()` resolves so the two values are reported as a snapshot. */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** Context tier for models that support multiple context-window sizes. */
    @JsonProperty("contextTier") ContextTier contextTier,
    /** Auto preference currently committed for the session. This can remain available while another model is selected so a later switch to `auto` can reuse it. */
    @JsonProperty("autoTier") AutoTier autoTier,
    /** Latest unclaimed Auto preference waiting for a future user turn. Null means the pending request is returning to provider-default routing. */
    @JsonProperty("pendingAutoTier") AutoTier pendingAutoTier,
    /** Auto preference currently claimed by an in-progress activation. Null means the activation is returning to provider-default routing. */
    @JsonProperty("activatingAutoTier") AutoTier activatingAutoTier
) {
}
