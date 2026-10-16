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
 * A discovered instance and one of its models to translate into provider configuration. Pass back the instance and model as returned by `session.providers.discover` and `session.providers.models.list`.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionProvidersModelsPrepareConfigurationParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The discovered instance that serves the model. */
    @JsonProperty("instance") ModelProviderInstance instance,
    /** The discovered model to configure. */
    @JsonProperty("model") DiscoveredModel model
) {
}
