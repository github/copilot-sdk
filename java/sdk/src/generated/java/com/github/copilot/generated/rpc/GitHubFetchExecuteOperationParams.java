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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Arguments for a named authenticated operation.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubFetchExecuteOperationParams(
    /** A fresh request id. */
    @JsonProperty("requestId") Long requestId,
    /** Session whose auth manager to use. Omission selects the process manager. */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque selector for a live host-owned credential binding. Only the owning host connection may use it. */
    @JsonProperty("authOwnerId") String authOwnerId,
    /** The operation to execute. */
    @JsonProperty("operation") GitHubFetchOperation operation,
    /** Named operation arguments, such as owner, repo, or jobId. */
    @JsonProperty("params") Map<String, String> params,
    /** JSON request body, preserved as supplied. */
    @JsonProperty("bodyJson") String bodyJson
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GitHubFetchExecuteOperationParams {}

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
        private String sessionId;
        private String authOwnerId;
        private GitHubFetchOperation operation;
        private Map<String, String> params;
        private String bodyJson;

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
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code authOwnerId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder authOwnerId(String value) {
            this.authOwnerId = value;
            return this;
        }

        /**
         * Sets the {@code operation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder operation(GitHubFetchOperation value) {
            this.operation = value;
            return this;
        }

        /**
         * Sets the {@code params} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder params(Map<String, String> value) {
            this.params = value;
            return this;
        }

        /**
         * Sets the {@code bodyJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder bodyJson(String value) {
            this.bodyJson = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubFetchExecuteOperationParams build() {
            if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
            if (operation == null) throw new IllegalStateException("Missing required property: operation");
            if (params == null) throw new IllegalStateException("Missing required property: params");
            return new GitHubFetchExecuteOperationParams(requestId, sessionId, authOwnerId, operation, params, bodyJson);
        }
    }
}
