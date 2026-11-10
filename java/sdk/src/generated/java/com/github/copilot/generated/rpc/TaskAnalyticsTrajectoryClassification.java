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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * One terminal classification of a worker-local unit, including categorical output and actual judge configuration when available.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskAnalyticsTrajectoryClassification(
    /** Registered classifier name. */
    @JsonProperty("classifier") String classifier,
    /** Classifier prompt/schema fingerprint; not a complete runtime/configuration identity. */
    @JsonProperty("classifierVersion") Long classifierVersion,
    /** Revision of this classifier's closed vocabulary, when applicable. */
    @JsonProperty("taxonomyVersion") Long taxonomyVersion,
    /** Unit scope: prompt, turn, task, or session. */
    @JsonProperty("scope") String scope,
    /** Analysis-local unit identity. Session snapshots can share an identity. */
    @JsonProperty("unitId") String unitId,
    /** One-based unit ordinal within its scope and this analysis request. */
    @JsonProperty("ordinal") Long ordinal,
    /** Source event opening this unit, when applicable. */
    @JsonProperty("anchorEventId") String anchorEventId,
    /** Why the unit closed; EOF is not reported as a successful completed turn. */
    @JsonProperty("closeReason") String closeReason,
    /** Terminal status: success, declined, abandoned, or failed. */
    @JsonProperty("status") String status,
    /** Unrestricted categorical labels, empty when classification did not succeed. */
    @JsonProperty("labels") Map<String, String> labels,
    /** Unrestricted numerical verdict measurements, such as confidence. */
    @JsonProperty("metrics") Map<String, Double> metrics,
    /** Whether retention caps truncated the unit's text. */
    @JsonProperty("textTruncated") Boolean textTruncated,
    /** Number of unit boundaries inferred without a successful segmentation judgment. */
    @JsonProperty("unresolvedBoundaries") Long unresolvedBoundaries,
    /** First member-turn ordinal for task/session units. */
    @JsonProperty("firstMemberOrdinal") Long firstMemberOrdinal,
    /** Last member-turn ordinal for task/session units. */
    @JsonProperty("lastMemberOrdinal") Long lastMemberOrdinal,
    /** Resolved judge model when available, including failed inference calls; absent when model resolution did not complete. */
    @JsonProperty("model") String model,
    /** Interaction ID used for this row's judge call. New attempts never reuse archived IDs; preserved outcomes keep their original value. */
    @JsonProperty("interactionId") String interactionId,
    /** Why the canonical classifier model policy selected this judge. */
    @JsonProperty("modelSelectionReason") String modelSelectionReason,
    /** Actual judge reasoning effort, when reported. */
    @JsonProperty("reasoningEffort") String reasoningEffort,
    /** Consumed input tokens, when reported. */
    @JsonProperty("inputTokens") Double inputTokens,
    /** Consumed output tokens, when reported. */
    @JsonProperty("outputTokens") Double outputTokens,
    /** Cached input tokens, when reported. */
    @JsonProperty("cachedInputTokens") Double cachedInputTokens,
    /** Stable failure/decline kind, never a raw error or judge response. */
    @JsonProperty("errorKind") String errorKind,
    /** HTTP failure status from inference, when available. Not inferred from message text. */
    @JsonProperty("httpStatus") Long httpStatus,
    /** Whether a failed inference may be retried. Absent for failures without an HTTP response. Does not initiate an automatic retry. */
    @JsonProperty("retryable") Boolean retryable,
    /** Server-requested minimum delay in seconds before retry, including multi-hour delays. Absent for missing or malformed Retry-After headers. */
    @JsonProperty("retryAfterSeconds") Double retryAfterSeconds
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public TaskAnalyticsTrajectoryClassification {}

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
        private String classifier;
        private Long classifierVersion;
        private Long taxonomyVersion;
        private String scope;
        private String unitId;
        private Long ordinal;
        private String anchorEventId;
        private String closeReason;
        private String status;
        private Map<String, String> labels;
        private Map<String, Double> metrics;
        private Boolean textTruncated;
        private Long unresolvedBoundaries;
        private Long firstMemberOrdinal;
        private Long lastMemberOrdinal;
        private String model;
        private String interactionId;
        private String modelSelectionReason;
        private String reasoningEffort;
        private Double inputTokens;
        private Double outputTokens;
        private Double cachedInputTokens;
        private String errorKind;
        private Long httpStatus;
        private Boolean retryable;
        private Double retryAfterSeconds;

        /**
         * Sets the {@code classifier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder classifier(String value) {
            this.classifier = value;
            return this;
        }

        /**
         * Sets the {@code classifierVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder classifierVersion(Long value) {
            this.classifierVersion = value;
            return this;
        }

        /**
         * Sets the {@code taxonomyVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder taxonomyVersion(Long value) {
            this.taxonomyVersion = value;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder scope(String value) {
            this.scope = value;
            return this;
        }

        /**
         * Sets the {@code unitId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder unitId(String value) {
            this.unitId = value;
            return this;
        }

        /**
         * Sets the {@code ordinal} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder ordinal(Long value) {
            this.ordinal = value;
            return this;
        }

        /**
         * Sets the {@code anchorEventId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder anchorEventId(String value) {
            this.anchorEventId = value;
            return this;
        }

        /**
         * Sets the {@code closeReason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder closeReason(String value) {
            this.closeReason = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder status(String value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code labels} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder labels(Map<String, String> value) {
            this.labels = value;
            return this;
        }

        /**
         * Sets the {@code metrics} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder metrics(Map<String, Double> value) {
            this.metrics = value;
            return this;
        }

        /**
         * Sets the {@code textTruncated} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder textTruncated(Boolean value) {
            this.textTruncated = value;
            return this;
        }

        /**
         * Sets the {@code unresolvedBoundaries} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder unresolvedBoundaries(Long value) {
            this.unresolvedBoundaries = value;
            return this;
        }

        /**
         * Sets the {@code firstMemberOrdinal} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder firstMemberOrdinal(Long value) {
            this.firstMemberOrdinal = value;
            return this;
        }

        /**
         * Sets the {@code lastMemberOrdinal} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder lastMemberOrdinal(Long value) {
            this.lastMemberOrdinal = value;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code interactionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder interactionId(String value) {
            this.interactionId = value;
            return this;
        }

        /**
         * Sets the {@code modelSelectionReason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder modelSelectionReason(String value) {
            this.modelSelectionReason = value;
            return this;
        }

        /**
         * Sets the {@code reasoningEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reasoningEffort(String value) {
            this.reasoningEffort = value;
            return this;
        }

        /**
         * Sets the {@code inputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder inputTokens(Double value) {
            this.inputTokens = value;
            return this;
        }

        /**
         * Sets the {@code outputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder outputTokens(Double value) {
            this.outputTokens = value;
            return this;
        }

        /**
         * Sets the {@code cachedInputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder cachedInputTokens(Double value) {
            this.cachedInputTokens = value;
            return this;
        }

        /**
         * Sets the {@code errorKind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder errorKind(String value) {
            this.errorKind = value;
            return this;
        }

        /**
         * Sets the {@code httpStatus} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder httpStatus(Long value) {
            this.httpStatus = value;
            return this;
        }

        /**
         * Sets the {@code retryable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder retryable(Boolean value) {
            this.retryable = value;
            return this;
        }

        /**
         * Sets the {@code retryAfterSeconds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder retryAfterSeconds(Double value) {
            this.retryAfterSeconds = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public TaskAnalyticsTrajectoryClassification build() {
            if (classifier == null) throw new IllegalStateException("Missing required property: classifier");
            if (classifierVersion == null) throw new IllegalStateException("Missing required property: classifierVersion");
            if (scope == null) throw new IllegalStateException("Missing required property: scope");
            if (unitId == null) throw new IllegalStateException("Missing required property: unitId");
            if (ordinal == null) throw new IllegalStateException("Missing required property: ordinal");
            if (closeReason == null) throw new IllegalStateException("Missing required property: closeReason");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            if (labels == null) throw new IllegalStateException("Missing required property: labels");
            if (metrics == null) throw new IllegalStateException("Missing required property: metrics");
            if (textTruncated == null) throw new IllegalStateException("Missing required property: textTruncated");
            if (unresolvedBoundaries == null) throw new IllegalStateException("Missing required property: unresolvedBoundaries");
            return new TaskAnalyticsTrajectoryClassification(classifier, classifierVersion, taxonomyVersion, scope, unitId, ordinal, anchorEventId, closeReason, status, labels, metrics, textTruncated, unresolvedBoundaries, firstMemberOrdinal, lastMemberOrdinal, model, interactionId, modelSelectionReason, reasoningEffort, inputTokens, outputTokens, cachedInputTokens, errorKind, httpStatus, retryable, retryAfterSeconds);
        }
    }
}
