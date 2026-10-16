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
 * Typed effective values of managed settings. Each field mirrors the managed-settings schema key of the same name; more keys are added as they are typed.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedSettingsValues(
    /** Managed default model identifier. When model availability was resolved, aliases and family names are projected to a concrete available model ID; otherwise the configured value is returned. */
    @JsonProperty("model") String model,
    /** Managed Auto routing preference, used when the selected model is `auto`. */
    @JsonProperty("autoTier") AutoTier autoTier,
    /** Managed Ensemble preference. Takes precedence over autoTier even when unmanaged, which means no managed tier and forbids legacy fallback. Legacy Auto values are mapped only when no managed source supplies ensembleTier. */
    @JsonProperty("ensembleTier") ManagedEnsembleTier ensembleTier,
    /** Managed reasoning-effort default for the managed concrete model. The runtime clamps it to an entitled effort when model availability is known. */
    @JsonProperty("effortLevel") String effortLevel,
    /** Managed context-tier default for the managed concrete model. */
    @JsonProperty("contextTier") ContextTier contextTier
) {
}
