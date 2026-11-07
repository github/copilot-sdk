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
 * Models offered for agent conversations by one provider instance. Adapters exclude known-incompatible models, but retain candidates with unknown capabilities. Listing does not guarantee compatibility.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionProvidersModelsListResult(
    /** Typed operation outcome. */
    @JsonProperty("outcome") ModelProviderOperationOutcome outcome,
    /** Provider-native models in provider order. */
    @JsonProperty("models") List<DiscoveredModel> models
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionProvidersModelsListResult {}

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
        private ModelProviderOperationOutcome outcome;
        private List<DiscoveredModel> models;

        /**
         * Sets the {@code outcome} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder outcome(ModelProviderOperationOutcome value) {
            this.outcome = value;
            return this;
        }

        /**
         * Sets the {@code models} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder models(List<DiscoveredModel> value) {
            this.models = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionProvidersModelsListResult build() {
            if (outcome == null) throw new IllegalStateException("Missing required property: outcome");
            if (models == null) throw new IllegalStateException("Missing required property: models");
            return new SessionProvidersModelsListResult(outcome, models);
        }
    }
}
