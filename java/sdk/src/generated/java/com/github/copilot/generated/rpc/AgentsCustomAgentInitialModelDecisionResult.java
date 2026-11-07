/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The model to switch to, and the warning to show when the agent's preference could not be met.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record AgentsCustomAgentInitialModelDecisionResult(
    /** The first available model that matches the agent's preferences. Absent when none of the requested models is available. */
    @JsonProperty("targetModel") String targetModel,
    /** The reasoning effort attached to the selected model preference. Absent when that preference does not specify an effort. */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** What to tell the user about an unmet preference. Absent when the preference was met. A warning with no `targetModel` means the agent's models are all unavailable. */
    @JsonProperty("warning") String warning
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    AgentsCustomAgentInitialModelDecisionResult {}

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
        private String targetModel;
        private String reasoningEffort;
        private String warning;

        /**
         * Sets the {@code targetModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder targetModel(String value) {
            this.targetModel = value;
            return this;
        }

        /**
         * Sets the {@code reasoningEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reasoningEffort(String value) {
            this.reasoningEffort = value;
            return this;
        }

        /**
         * Sets the {@code warning} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder warning(String value) {
            this.warning = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AgentsCustomAgentInitialModelDecisionResult build() {
            return new AgentsCustomAgentInitialModelDecisionResult(targetModel, reasoningEffort, warning);
        }
    }
}
