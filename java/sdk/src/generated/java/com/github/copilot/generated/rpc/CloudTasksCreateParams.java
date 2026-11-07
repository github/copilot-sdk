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
 * Describes the cloud coding-agent task to start and the credential to start it under. Target it either by repository, supplying both `repositoryOwner` and `repositoryName`, or by `owner` alone for an account-level run.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CloudTasksCreateParams(
    /** Explicit Mission Control endpoint selected by the host. Omission derives it from the identity. */
    @JsonProperty("baseUrl") String baseUrl,
    /** The credential the task is created under, carried opaquely because its shape is the host's own and the runtime only resolves a token and an API endpoint from it. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("authInfo") Object authInfo,
    /** Selects which stored credential to resolve when the host holds more than one. Omit it to resolve the default for the supplied `authInfo`. */
    @JsonProperty("authContextId") String authContextId,
    /** Name of the embedding client, reported to the service so it can attribute the request. */
    @JsonProperty("integrationId") String integrationId,
    /** Account or organization owning the repository the task runs against. Supply it together with `repositoryName`. */
    @JsonProperty("repositoryOwner") String repositoryOwner,
    /** Name of the repository the task runs against. Supply it together with `repositoryOwner`. */
    @JsonProperty("repositoryName") String repositoryName,
    /** Account the task is created for when it is not tied to a repository. Supply it only when `repositoryOwner` and `repositoryName` are omitted. */
    @JsonProperty("owner") String owner
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    CloudTasksCreateParams {}

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
        private String baseUrl;
        private Object authInfo;
        private boolean $presence1;
        private String authContextId;
        private String integrationId;
        private String repositoryOwner;
        private String repositoryName;
        private String owner;

        /**
         * Sets the {@code baseUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder baseUrl(String value) {
            this.baseUrl = value;
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
         * Sets the {@code authContextId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder authContextId(String value) {
            this.authContextId = value;
            return this;
        }

        /**
         * Sets the {@code integrationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder integrationId(String value) {
            this.integrationId = value;
            return this;
        }

        /**
         * Sets the {@code repositoryOwner} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder repositoryOwner(String value) {
            this.repositoryOwner = value;
            return this;
        }

        /**
         * Sets the {@code repositoryName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder repositoryName(String value) {
            this.repositoryName = value;
            return this;
        }

        /**
         * Sets the {@code owner} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder owner(String value) {
            this.owner = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CloudTasksCreateParams build() {
            if (!$presence1) throw new IllegalStateException("Missing required property: authInfo");
            if (integrationId == null) throw new IllegalStateException("Missing required property: integrationId");
            return new CloudTasksCreateParams(baseUrl, authInfo, authContextId, integrationId, repositoryOwner, repositoryName, owner);
        }
    }
}
