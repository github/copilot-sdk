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
 * Configured names of the GitHub host to resolve.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record AccountGetHostUrlParams(
    /** Host from the Copilot-specific setting, which wins when both are set. A bare hostname is normalized to `https://`. */
    @JsonProperty("copilotGhHost") String copilotGhHost,
    /** Host from the general GitHub CLI setting, used when the Copilot-specific one is absent. */
    @JsonProperty("ghHost") String ghHost
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    AccountGetHostUrlParams {}

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
        private String copilotGhHost;
        private String ghHost;

        /**
         * Sets the {@code copilotGhHost} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder copilotGhHost(String value) {
            this.copilotGhHost = value;
            return this;
        }

        /**
         * Sets the {@code ghHost} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder ghHost(String value) {
            this.ghHost = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AccountGetHostUrlParams build() {
            return new AccountGetHostUrlParams(copilotGhHost, ghHost);
        }
    }
}
