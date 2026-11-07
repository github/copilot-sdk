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
 * Registry search terms, the credential to search under, and the request id that makes the search cancellable.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpRegistrySearchParams(
    /** Request ID from `mcp.registry.allocateRequestId`. The search refuses unknown, reclaimed, or canceled IDs and IDs that another search or request already uses. */
    @JsonProperty("requestId") Long requestId,
    /** The credential the search runs under, carried opaquely. Hosts usually send credential-free `AuthIdentity`; a `token` identity can be resolved only when it embeds a token, while `env` and `gh-cli` identities can use a token embedded in the request first. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("authInfo") Object authInfo,
    /** Free-text query. Omitted or empty asks the registry for its top servers rather than searching. A value that is not a string is refused. */
    @JsonProperty("query") String query,
    /** Repository used for the policy lookup, as `owner/name`. The policy selects the registry URL and whether the user token goes to the registry. The registry receives this repository only when the policy entry lists it as required context. A value that is not a string is refused. */
    @JsonProperty("repository") String repository,
    /** Maximum number of servers to return. */
    @JsonProperty("limit") Long limit
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    McpRegistrySearchParams {}

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
        private Long requestId;
        private Object authInfo;
        private boolean $presence1;
        private String query;
        private String repository;
        private Long limit;

        /**
         * Sets the {@code requestId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requestId(Long value) {
            this.requestId = value;
            return this;
        }

        /**
         * Sets the {@code authInfo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder authInfo(Object value) {
            this.authInfo = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code query} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder query(String value) {
            this.query = value;
            return this;
        }

        /**
         * Sets the {@code repository} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder repository(String value) {
            this.repository = value;
            return this;
        }

        /**
         * Sets the {@code limit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder limit(Long value) {
            this.limit = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpRegistrySearchParams build() {
            if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
            if (!$presence1) throw new IllegalStateException("Missing required property: authInfo");
            if (limit == null) throw new IllegalStateException("Missing required property: limit");
            return new McpRegistrySearchParams(requestId, authInfo, query, repository, limit);
        }
    }
}
