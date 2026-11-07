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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Outcome of an owner listing. Exactly one of `owners` and `message` is present, except that `throwError` reports a failure the caller is expected to raise rather than render.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubOwnersListResult(
    /** The owners, on success: the authenticated user first, then the organizations they belong to. */
    @JsonProperty("owners") List<GitHubOwnerOption> owners,
    /** Why no owners could be listed, phrased for a user. Present when the listing failed in a way the caller should render rather than raise. */
    @JsonProperty("message") String message,
    /** A line the caller should log. Present only alongside `message`, and only for failures worth recording. */
    @JsonProperty("warning") String warning,
    /** A malformed request or an unreadable credential, which the caller raises instead of rendering. Kept a field rather than a dispatch error so it stays distinct from `message`, which the caller renders. */
    @JsonProperty("throwError") String throwError
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GitHubOwnersListResult {}

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
        private List<GitHubOwnerOption> owners;
        private String message;
        private String warning;
        private String throwError;

        /**
         * Sets the {@code owners} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder owners(List<GitHubOwnerOption> value) {
            this.owners = value;
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
         * Sets the {@code warning} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder warning(String value) {
            this.warning = value;
            return this;
        }

        /**
         * Sets the {@code throwError} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder throwError(String value) {
            this.throwError = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubOwnersListResult build() {
            return new GitHubOwnersListResult(owners, message, warning, throwError);
        }
    }
}
