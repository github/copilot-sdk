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
 * Redacted job settings for a session. The job nonce is excluded.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionSettingsJobSnapshot(
    /** GitHub Actions event type for the job. */
    @JsonProperty("eventType") String eventType,
    /** Whether this is the workflow's trigger job. */
    @JsonProperty("isTriggerJob") Boolean isTriggerJob,
    /** Availability of job-specific built-in tools. */
    @JsonProperty("builtInToolAvailability") SessionSettingsBuiltInToolAvailabilitySnapshot builtInToolAvailability
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionSettingsJobSnapshot {}

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
        private String eventType;
        private Boolean isTriggerJob;
        private SessionSettingsBuiltInToolAvailabilitySnapshot builtInToolAvailability;

        /**
         * Sets the {@code eventType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder eventType(String value) {
            this.eventType = value;
            return this;
        }

        /**
         * Sets the {@code isTriggerJob} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isTriggerJob(Boolean value) {
            this.isTriggerJob = value;
            return this;
        }

        /**
         * Sets the {@code builtInToolAvailability} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder builtInToolAvailability(SessionSettingsBuiltInToolAvailabilitySnapshot value) {
            this.builtInToolAvailability = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionSettingsJobSnapshot build() {
            return new SessionSettingsJobSnapshot(eventType, isTriggerJob, builtInToolAvailability);
        }
    }
}
