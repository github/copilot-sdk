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
 * The models a custom agent asks for, and the models actually available.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record AgentsCustomAgentInitialModelDecisionParams(
    /** The agent's declared `model:` entry, serialized. A single name or an ordered list of acceptable names. */
    @JsonProperty("agentModelsJson") String agentModelsJson,
    /** The models available to this session, serialized in the shape the model list carries. */
    @JsonProperty("availableModelsJson") String availableModelsJson
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    AgentsCustomAgentInitialModelDecisionParams {}

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
        private String agentModelsJson;
        private String availableModelsJson;

        /**
         * Sets the {@code agentModelsJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder agentModelsJson(String value) {
            this.agentModelsJson = value;
            return this;
        }

        /**
         * Sets the {@code availableModelsJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder availableModelsJson(String value) {
            this.availableModelsJson = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AgentsCustomAgentInitialModelDecisionParams build() {
            if (agentModelsJson == null) throw new IllegalStateException("Missing required property: agentModelsJson");
            if (availableModelsJson == null) throw new IllegalStateException("Missing required property: availableModelsJson");
            return new AgentsCustomAgentInitialModelDecisionParams(agentModelsJson, availableModelsJson);
        }
    }
}
