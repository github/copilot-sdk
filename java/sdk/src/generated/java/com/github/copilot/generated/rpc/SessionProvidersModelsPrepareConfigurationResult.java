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

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionProvidersModelsPrepareConfigurationResult {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private NamedProviderConfig provider;
        private ProviderModelConfig model;
        private String selectionId;
        private ModelProviderConfigurationDisposition providerDisposition;
        private ModelProviderConfigurationDisposition modelDisposition;
        private List<ModelProviderWarning> warnings;

        /**
         * Sets the {@code provider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder provider(NamedProviderConfig value) {
            this.provider = value;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder model(ProviderModelConfig value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code selectionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder selectionId(String value) {
            this.selectionId = value;
            return this;
        }

        /**
         * Sets the {@code providerDisposition} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder providerDisposition(ModelProviderConfigurationDisposition value) {
            this.providerDisposition = value;
            return this;
        }

        /**
         * Sets the {@code modelDisposition} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder modelDisposition(ModelProviderConfigurationDisposition value) {
            this.modelDisposition = value;
            return this;
        }

        /**
         * Sets the {@code warnings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder warnings(List<ModelProviderWarning> value) {
            this.warnings = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionProvidersModelsPrepareConfigurationResult build() {
            if (provider == null) throw new IllegalStateException("Missing required property: provider");
            if (model == null) throw new IllegalStateException("Missing required property: model");
            if (selectionId == null) throw new IllegalStateException("Missing required property: selectionId");
            if (providerDisposition == null) throw new IllegalStateException("Missing required property: providerDisposition");
            if (modelDisposition == null) throw new IllegalStateException("Missing required property: modelDisposition");
            if (warnings == null) throw new IllegalStateException("Missing required property: warnings");
            return new SessionProvidersModelsPrepareConfigurationResult(provider, model, selectionId, providerDisposition, modelDisposition, warnings);
        }
    }
}
