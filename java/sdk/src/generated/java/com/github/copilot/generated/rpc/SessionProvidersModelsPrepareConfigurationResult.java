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
 * Provider configuration prepared from a discovered model. Preparing a plan changes nothing: it neither registers the model with the session nor writes durable configuration. To apply it, pass `provider` and `model` to `session.provider.add`, omitting whichever the dispositions report as already configured.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionProvidersModelsPrepareConfigurationResult(
    /** Provider connection prepared from the instance's inference metadata. Carries no credential; supply one if the endpoint requires it. */
    @JsonProperty("provider") NamedProviderConfig provider,
    /** Model definition prepared from the discovered model. Capability fields the provider did not report stay omitted rather than being asserted false. */
    @JsonProperty("model") ProviderModelConfig model,
    /** Provider-qualified selection id (`provider/id`) to pass to `switchTo` once the plan is applied. */
    @JsonProperty("selectionId") String selectionId,
    /** Whether `provider` still needs to be registered. When `alreadyConfigured`, a provider with the same endpoint is already registered and `provider` restates it under its existing name; adding it again is rejected as a duplicate. */
    @JsonProperty("providerDisposition") ModelProviderConfigurationDisposition providerDisposition,
    /** Whether `model` still needs to be registered. When `alreadyConfigured`, `selectionId` is already registered and the caller can select it without adding anything. */
    @JsonProperty("modelDisposition") ModelProviderConfigurationDisposition modelDisposition,
    /** Non-fatal warnings carried over from the discovered model, such as capabilities the provider did not report. */
    @JsonProperty("warnings") List<ModelProviderWarning> warnings
) {
}
