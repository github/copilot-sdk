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
 * Outcome of a release lookup. `releaseJson` is present only for `found`, and `message` only for the kinds that carry an explanation.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubReleaseLookupResult(
    /** How the lookup ended. */
    @JsonProperty("kind") GitHubReleaseLookupKind kind,
    /** The release as GitHub's own JSON, verbatim. Carried as text rather than a modelled type so the contract does not have to track every field of GitHub's release payload, and so a caller reads the same shape the REST API documents. */
    @JsonProperty("releaseJson") String releaseJson,
    /** Why the lookup produced no release, phrased for a user. */
    @JsonProperty("message") String message
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GitHubReleaseLookupResult {}

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
        private GitHubReleaseLookupKind kind;
        private String releaseJson;
        private String message;

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder kind(GitHubReleaseLookupKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code releaseJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder releaseJson(String value) {
            this.releaseJson = value;
            return this;
        }

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder message(String value) {
            this.message = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubReleaseLookupResult build() {
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            return new GitHubReleaseLookupResult(kind, releaseJson, message);
        }
    }
}
