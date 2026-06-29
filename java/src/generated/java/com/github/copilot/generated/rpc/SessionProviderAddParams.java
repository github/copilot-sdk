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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * BYOK providers and/or models to add to the session's registry at runtime. Both fields are optional; provide providers, models, or both.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionProviderAddParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Named BYOK provider connections to register, additive to any providers already in the registry. Each name must be unique across the registry and must not contain '/'. */
    @JsonProperty("providers") List<NamedProviderConfig> providers,
    /** BYOK model definitions to register. Each must reference a provider that is already registered or included in this same call. Selection ids (`provider/id`) must be unique across the registry. */
    @JsonProperty("models") List<ProviderModelConfig> models
) {
}
