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
 * Redacted, serializable view of session runtime settings for SDK boundary consumers. Secrets and raw feature flags are intentionally excluded.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionSettingsSnapshot(
    /** Agent runtime version selector copied from the session settings, such as `latest` or a runtime release identifier. */
    @JsonProperty("version") String version,
    /** Name of the SDK client that created the session. */
    @JsonProperty("clientName") String clientName,
    /** Session timeout in milliseconds. */
    @JsonProperty("timeoutMs") Double timeoutMs,
    /** Session start time as Unix epoch milliseconds. */
    @JsonProperty("startTimeMs") Double startTimeMs,
    /** Redacted repository and host settings. */
    @JsonProperty("repo") SessionSettingsRepoSnapshot repo,
    /** Redacted model routing settings. */
    @JsonProperty("model") SessionSettingsModelSnapshot model,
    /** Redacted validation and memory-tool settings. */
    @JsonProperty("validation") SessionSettingsValidationSnapshot validation,
    /** Redacted job settings. */
    @JsonProperty("job") SessionSettingsJobSnapshot job,
    /** Online-evaluation settings safe for SDK consumers. */
    @JsonProperty("onlineEvaluation") SessionSettingsOnlineEvaluationSnapshot onlineEvaluation
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionSettingsSnapshot {}

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
        private String version;
        private String clientName;
        private Double timeoutMs;
        private Double startTimeMs;
        private SessionSettingsRepoSnapshot repo;
        private SessionSettingsModelSnapshot model;
        private SessionSettingsValidationSnapshot validation;
        private SessionSettingsJobSnapshot job;
        private SessionSettingsOnlineEvaluationSnapshot onlineEvaluation;

        /**
         * Sets the {@code version} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder version(String value) {
            this.version = value;
            return this;
        }

        /**
         * Sets the {@code clientName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clientName(String value) {
            this.clientName = value;
            return this;
        }

        /**
         * Sets the {@code timeoutMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder timeoutMs(Double value) {
            this.timeoutMs = value;
            return this;
        }

        /**
         * Sets the {@code startTimeMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder startTimeMs(Double value) {
            this.startTimeMs = value;
            return this;
        }

        /**
         * Sets the {@code repo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder repo(SessionSettingsRepoSnapshot value) {
            this.repo = value;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder model(SessionSettingsModelSnapshot value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code validation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder validation(SessionSettingsValidationSnapshot value) {
            this.validation = value;
            return this;
        }

        /**
         * Sets the {@code job} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder job(SessionSettingsJobSnapshot value) {
            this.job = value;
            return this;
        }

        /**
         * Sets the {@code onlineEvaluation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder onlineEvaluation(SessionSettingsOnlineEvaluationSnapshot value) {
            this.onlineEvaluation = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionSettingsSnapshot build() {
            if (repo == null) throw new IllegalStateException("Missing required property: repo");
            if (model == null) throw new IllegalStateException("Missing required property: model");
            if (validation == null) throw new IllegalStateException("Missing required property: validation");
            if (job == null) throw new IllegalStateException("Missing required property: job");
            if (onlineEvaluation == null) throw new IllegalStateException("Missing required property: onlineEvaluation");
            return new SessionSettingsSnapshot(version, clientName, timeoutMs, startTimeMs, repo, model, validation, job, onlineEvaluation);
        }
    }
}
