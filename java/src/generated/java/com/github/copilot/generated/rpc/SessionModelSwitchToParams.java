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
 * Target model identifier and optional reasoning effort, summary, capability overrides, and context tier.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelSwitchToParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Model selection id to switch to, as returned by `list`. A bare id (e.g. `claude-sonnet-4.6`) names a Copilot (CAPI) model; a provider-qualified id (`provider/id`, e.g. `acme/claude-sonnet`) targets a registry BYOK model. */
    @JsonProperty("modelId") String modelId,
    /** Reasoning effort level to use for the model. "none" disables reasoning. */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** Reasoning summary mode to request for supported model clients */
    @JsonProperty("reasoningSummary") ReasoningSummary reasoningSummary,
    /** Output verbosity level to request for supported models */
    @JsonProperty("verbosity") Verbosity verbosity,
    /** Override individual model capabilities resolved by the runtime */
    @JsonProperty("modelCapabilities") ModelCapabilitiesOverride modelCapabilities,
    /** Explicit context tier for the selected model. `"default"` / `"long_context"` apply the requested tier; omit this field to use normal model behavior with no explicit tier. */
    @JsonProperty("contextTier") ContextTier contextTier,
    /** When true, defer this switch (enqueue it) if another model change is already queued, even when no turn is active — so it drains last (FIFO) and wins over the already-queued change. Intended for genuine user-initiated model selections; internal restore/reapply switches omit it and apply immediately when no turn is active. When no other model change is queued this has no effect (a switch still applies immediately unless a turn is active). */
    @JsonProperty("deferIfModelChangeQueued") Boolean deferIfModelChangeQueued
) {
}
