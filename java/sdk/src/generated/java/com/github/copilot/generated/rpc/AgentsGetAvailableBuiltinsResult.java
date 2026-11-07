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
import javax.annotation.processing.Generated;

/**
 * The shipped agents available under the requested flags.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record AgentsGetAvailableBuiltinsResult(
    /** Available shipped agents, in the runtime's own order. */
    @JsonProperty("agents") List<BuiltinAgentSummary> agents
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    AgentsGetAvailableBuiltinsResult {}

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
        private List<BuiltinAgentSummary> agents;

        /**
         * Sets the {@code agents} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder agents(List<BuiltinAgentSummary> value) {
            this.agents = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AgentsGetAvailableBuiltinsResult build() {
            if (agents == null) throw new IllegalStateException("Missing required property: agents");
            return new AgentsGetAvailableBuiltinsResult(agents);
        }
    }
}
