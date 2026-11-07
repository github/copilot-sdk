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
 * Availability of built-in job tools surfaced to boundary consumers.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionSettingsBuiltInToolAvailabilitySnapshot(
    /** Whether the report-progress tool is available. */
    @JsonProperty("reportProgress") Boolean reportProgress,
    /** Whether the create-pull-request tool is available. */
    @JsonProperty("createPullRequest") Boolean createPullRequest
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionSettingsBuiltInToolAvailabilitySnapshot {}

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
        private Boolean reportProgress;
        private Boolean createPullRequest;

        /**
         * Sets the {@code reportProgress} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reportProgress(Boolean value) {
            this.reportProgress = value;
            return this;
        }

        /**
         * Sets the {@code createPullRequest} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder createPullRequest(Boolean value) {
            this.createPullRequest = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionSettingsBuiltInToolAvailabilitySnapshot build() {
            return new SessionSettingsBuiltInToolAvailabilitySnapshot(reportProgress, createPullRequest);
        }
    }
}
