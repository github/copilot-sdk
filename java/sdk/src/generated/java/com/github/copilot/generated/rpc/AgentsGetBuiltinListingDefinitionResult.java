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
 * One shipped agent, projected for a listing.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record AgentsGetBuiltinListingDefinitionResult(
    /** The agent projected as a custom agent, serialized as JSON. It is a string rather than an object for the same reason as `getBuiltinDefinition`: the runtime parses the underlying definition with the agent schema's tolerant shape, which accepts keys this contract does not name. */
    @JsonProperty("definitionJson") String definitionJson
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    AgentsGetBuiltinListingDefinitionResult {}

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
        private String definitionJson;

        /**
         * Sets the {@code definitionJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder definitionJson(String value) {
            this.definitionJson = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AgentsGetBuiltinListingDefinitionResult build() {
            if (definitionJson == null) throw new IllegalStateException("Missing required property: definitionJson");
            return new AgentsGetBuiltinListingDefinitionResult(definitionJson);
        }
    }
}
