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
 * The authentication identity to enroll in Copilot Free.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record AccountSubscribeCopilotFreeParams(
    /** Identity to enroll, in the shape `session.gitHubAuth.getCurrentAuthInfo` returns. The runtime resolves the credential for it, so no credential is sent. */
    @JsonProperty("identity") AuthIdentity identity
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    AccountSubscribeCopilotFreeParams {}

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
        private AuthIdentity identity;

        /**
         * Sets the {@code identity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder identity(AuthIdentity value) {
            this.identity = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AccountSubscribeCopilotFreeParams build() {
            if (identity == null) throw new IllegalStateException("Missing required property: identity");
            return new AccountSubscribeCopilotFreeParams(identity);
        }
    }
}
