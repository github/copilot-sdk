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
 * List of Copilot models available to the resolved user, including capabilities and billing metadata.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelsListResult(
    /** List of available models with full metadata */
    @JsonProperty("models") List<Model> models,
    /** Ordered Auto routing preferences discovered for the resolved account. Present only
when CAPI returned Auto metadata; same shape and semantics as `SessionModelList.auto`. */
    @JsonProperty("auto") AutoTierMetadata auto,
    /** The runtime's default image model, separate from conversation models, with its availability for the resolved account. The default is reported even when unavailable; clients choose whether to display it. These entries must not be selected as chat models. Absent on runtimes that do not expose this catalog. */
    @JsonProperty("imageModels") List<ImageToolModel> imageModels,
    /** Decision models the resolved account can call with `decisions.ask`, separate from conversation models, with each model's effective limits. Absent when the `DECISIONS_API` feature flag is off or the runtime does not expose this catalog; empty when the account has none. These entries must not be selected as chat models. */
    @CopilotExperimental
    @JsonProperty("decisionModels") List<DecisionModelInfo> decisionModels
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelsListResult {}

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
        private List<Model> models;
        private AutoTierMetadata auto;
        private List<ImageToolModel> imageModels;
        private List<DecisionModelInfo> decisionModels;

        /**
         * Sets the {@code models} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder models(List<Model> value) {
            this.models = value;
            return this;
        }

        /**
         * Sets the {@code auto} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder auto(AutoTierMetadata value) {
            this.auto = value;
            return this;
        }

        /**
         * Sets the {@code imageModels} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder imageModels(List<ImageToolModel> value) {
            this.imageModels = value;
            return this;
        }

        /**
         * Sets the {@code decisionModels} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder decisionModels(List<DecisionModelInfo> value) {
            this.decisionModels = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelsListResult build() {
            if (models == null) throw new IllegalStateException("Missing required property: models");
            return new ModelsListResult(models, auto, imageModels, decisionModels);
        }
    }

    /**
     * Creates a record with the components it had before later optional fields were added.
     * @deprecated Use named construction via {@link #builder()}.
     * @param models List of available models with full metadata
     * @param auto Ordered Auto routing preferences discovered for the resolved account. Present only when CAPI returned Auto metadata; same shape and semantics as `SessionModelList.auto`.
     * @param imageModels The runtime's default image model, separate from conversation models, with its availability for the resolved account. The default is reported even when unavailable; clients choose whether to display it. These entries must not be selected as chat models. Absent on runtimes that do not expose this catalog.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelsListResult(
        List<Model> models,
        AutoTierMetadata auto,
        List<ImageToolModel> imageModels
    ) {
        this(models, auto, imageModels, null);
    }
}
