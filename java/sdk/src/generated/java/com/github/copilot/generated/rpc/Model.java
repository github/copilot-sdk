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
    /** Model vendor as the Copilot API reports it, for example "Anthropic" or "Azure OpenAI". Open vocabulary, passed through unchanged. It can name the vendor that serves the model instead of the one that built it, or a label that is not a vendor, such as "Experimental". Absent when the Copilot API reports no vendor. */
    @JsonProperty("vendor") String vendor,
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
    @JsonProperty("warningMessages") List<ModelMessage> warningMessages,
    /** The native model provider that produced this model, as a neutral reference (opaque id, human-readable label, and provider kind). Preserved by `session.model.list` and `server.models.list` when supplied by the catalog producer; omitted for unattributed and configured BYOK models. This identifies the model source, not an account or agent harness. */
    @JsonProperty("provider") ModelProviderRef provider
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public Model {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private String id;
        private String name;
        private ModelCapabilities capabilities;
        private Map<String, Object> metadata;
        private String vendor;
        private ModelPolicy policy;
        private ModelBilling billing;
        private List<String> supportedReasoningEfforts;
        private String defaultReasoningEffort;
        private List<String> supportedContextTiers;
        private ModelPickerCategory modelPickerCategory;
        private ModelPickerPriceCategory modelPickerPriceCategory;
        private ModelWarningText warningText;
        private List<ModelMessage> infoMessages;
        private List<ModelMessage> warningMessages;
        private ModelProviderRef provider;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code capabilities} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder capabilities(ModelCapabilities value) {
            this.capabilities = value;
            return this;
        }

        /**
         * Sets the {@code metadata} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder metadata(Map<String, Object> value) {
            this.metadata = value;
            return this;
        }

        /**
         * Sets the {@code vendor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder vendor(String value) {
            this.vendor = value;
            return this;
        }

        /**
         * Sets the {@code policy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policy(ModelPolicy value) {
            this.policy = value;
            return this;
        }

        /**
         * Sets the {@code billing} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder billing(ModelBilling value) {
            this.billing = value;
            return this;
        }

        /**
         * Sets the {@code supportedReasoningEfforts} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder supportedReasoningEfforts(List<String> value) {
            this.supportedReasoningEfforts = value;
            return this;
        }

        /**
         * Sets the {@code defaultReasoningEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder defaultReasoningEffort(String value) {
            this.defaultReasoningEffort = value;
            return this;
        }

        /**
         * Sets the {@code supportedContextTiers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder supportedContextTiers(List<String> value) {
            this.supportedContextTiers = value;
            return this;
        }

        /**
         * Sets the {@code modelPickerCategory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelPickerCategory(ModelPickerCategory value) {
            this.modelPickerCategory = value;
            return this;
        }

        /**
         * Sets the {@code modelPickerPriceCategory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelPickerPriceCategory(ModelPickerPriceCategory value) {
            this.modelPickerPriceCategory = value;
            return this;
        }

        /**
         * Sets the {@code warningText} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder warningText(ModelWarningText value) {
            this.warningText = value;
            return this;
        }

        /**
         * Sets the {@code infoMessages} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder infoMessages(List<ModelMessage> value) {
            this.infoMessages = value;
            return this;
        }

        /**
         * Sets the {@code warningMessages} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder warningMessages(List<ModelMessage> value) {
            this.warningMessages = value;
            return this;
        }

        /**
         * Sets the {@code provider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder provider(ModelProviderRef value) {
            this.provider = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public Model build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (capabilities == null) throw new IllegalStateException("Missing required property: capabilities");
            return new Model(id, name, capabilities, metadata, vendor, policy, billing, supportedReasoningEfforts, defaultReasoningEffort, supportedContextTiers, modelPickerCategory, modelPickerPriceCategory, warningText, infoMessages, warningMessages, provider);
        }
    }
}
