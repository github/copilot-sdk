/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Copilot model metadata, including identifier, display name, capabilities, policy, billing, reasoning efforts, and picker categories.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Model(
    /** Model identifier (e.g., "claude-sonnet-4.5") */
    @JsonProperty("id") String id,
    /** Display name */
    @JsonProperty("name") String name,
    /** Model capabilities and limits */
    @JsonProperty("capabilities") ModelCapabilities capabilities,
    /** Provider-supplied model metadata. Keys and JSON-compatible values are preserved unchanged. This is factual metadata published by the model provider; it carries no picker or UX semantics. */
    @JsonProperty("metadata") Map<String, Object> metadata,
    /** Policy state (if applicable) */
    @JsonProperty("policy") ModelPolicy policy,
    /** Billing information */
    @JsonProperty("billing") ModelBilling billing,
    /** Supported reasoning effort levels (only present if model supports reasoning effort) */
    @JsonProperty("supportedReasoningEfforts") List<String> supportedReasoningEfforts,
    /** Default reasoning effort level (only present if model supports reasoning effort) */
    @JsonProperty("defaultReasoningEffort") String defaultReasoningEffort,
    /** Context-window tiers this model offers, when the provider advertises them independently of tiered token pricing. Copilot models carry their tiers in `billing.tokenPrices`; a provider that has no pricing to publish (an agent host reached over AHP, for example) declares them here instead, so the model picker can still offer the tier toggle. */
    @JsonProperty("supportedContextTiers") List<String> supportedContextTiers,
    /** Model capability category for grouping in the model picker */
    @JsonProperty("modelPickerCategory") ModelPickerCategory modelPickerCategory,
    /** Relative cost tier for token-based billing users */
    @JsonProperty("modelPickerPriceCategory") ModelPickerPriceCategory modelPickerPriceCategory,
    /** Warning text the service requires hosts to surface for this model. Present only when the service published at least one warning. */
    @JsonProperty("warningText") ModelWarningText warningText,
    /** Informational notices the service published for this model, such as an upcoming change or a recommended alternative. Present only when the service published at least one notice. Hosts should surface these without implying anything is wrong with the model. */
    @JsonProperty("infoMessages") List<ModelMessage> infoMessages,
    /** Warnings the service published for this model, such as a deprecated client version. Present only when the service published at least one warning. The model remains usable; hosts should surface these as advisory rather than blocking. */
    @JsonProperty("warningMessages") List<ModelMessage> warningMessages
) {
}
