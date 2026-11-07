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
 * Indicates whether the credential update succeeded.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionGitHubAuthSetCredentialsResult(
    /** Whether the operation succeeded */
    @JsonProperty("success") Boolean success,
    /** Whether the session ended up with a populated `copilotUser` for the installed credentials. `true` when the supplied credential already carried `copilotUser` or it was successfully re-resolved server-side. `false` when the credential is installed without `copilotUser` — either re-resolution failed, or the variant cannot be re-resolved from the credential alone (only the raw-token variants `token`, `env`, and `gh-cli` can). In both `false` cases the token swap still applied, but plan/quota/billing metadata is degraded. Present whenever a credential was supplied; omitted only when no credential was supplied (no-op call). */
    @JsonProperty("copilotUserResolved") Boolean copilotUserResolved
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionGitHubAuthSetCredentialsResult {}

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
        private Boolean success;
        private Boolean copilotUserResolved;

        /**
         * Sets the {@code success} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder success(Boolean value) {
            this.success = value;
            return this;
        }

        /**
         * Sets the {@code copilotUserResolved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder copilotUserResolved(Boolean value) {
            this.copilotUserResolved = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionGitHubAuthSetCredentialsResult build() {
            if (success == null) throw new IllegalStateException("Missing required property: success");
            return new SessionGitHubAuthSetCredentialsResult(success, copilotUserResolved);
        }
    }
}
