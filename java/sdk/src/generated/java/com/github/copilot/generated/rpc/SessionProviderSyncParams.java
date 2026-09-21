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
 * Authoritative BYOK provider and model registry snapshot to apply atomically to the session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionProviderSyncParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Named BYOK provider connection snapshot. Providers absent from this list are removed. */
    @JsonProperty("providers") List<NamedProviderConfig> providers,
    /** BYOK model definition snapshot. Models absent from this list are removed. */
    @JsonProperty("models") List<ProviderModelConfig> models
) {
}
