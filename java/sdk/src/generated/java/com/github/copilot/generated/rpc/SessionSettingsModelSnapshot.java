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
 * Redacted model routing settings for a session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionSettingsModelSnapshot(
    /** Selected model identifier. */
    @JsonProperty("model") String model,
    /** Default reasoning effort for the selected model. */
    @JsonProperty("defaultReasoningEffort") String defaultReasoningEffort,
    /** Agent job identifier for the session. */
    @JsonProperty("instanceId") String instanceId,
    /** Agent service callback URL for job and progress updates. */
    @JsonProperty("callbackUrl") String callbackUrl
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionSettingsModelSnapshot {}

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
        private String model;
        private String defaultReasoningEffort;
        private String instanceId;
        private String callbackUrl;

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code defaultReasoningEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder defaultReasoningEffort(String value) {
            this.defaultReasoningEffort = value;
            return this;
        }

        /**
         * Sets the {@code instanceId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder instanceId(String value) {
            this.instanceId = value;
            return this;
        }

        /**
         * Sets the {@code callbackUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder callbackUrl(String value) {
            this.callbackUrl = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionSettingsModelSnapshot build() {
            return new SessionSettingsModelSnapshot(model, defaultReasoningEffort, instanceId, callbackUrl);
        }
    }
}
