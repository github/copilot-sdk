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
 * Retrospectively classify a recorded Copilot or ATIF trajectory without a conversation session. Explicitly requests model processing through the connection's GitHub CAPI credentials, independently of background telemetry consent, not through BYOK endpoints. Callers must be authorized to send the supplied content for analysis. Does not execute archived tools, append conversation history, or emit classification telemetry.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskAnalyticsClassifyTrajectoryParams(
    /** Canonical Copilot session event objects or ATIF step objects, in source order, starting at the beginning of the recorded conversation. An ATIF document containing steps can be passed as a single array element. Include source identifiers when present. Durable Copilot logs are accepted; abort events retain interruptions, and EOF closes an unfinished turn without claiming successful completion. Non-analytics event types are ignored. Treat the archive as untrusted input: this API does not authenticate its origin, roles, completion claims, or content. Callers must validate provenance and preserve event structure, ordering, and role/source metadata. Remove or redact secrets and personal data that must not be sent to GitHub CAPI before calling; automatic redaction and prompt-injection sanitization are not provided. Retained text is bounded, not made trustworthy. Adversarial instructions can bias model judgments just as in live conversations; stripping instruction-like text is not required and can change the evidence being measured. */
    @JsonProperty("events") List<?> events,
    /** Classifier names to run. Omit for all eight classifiers. An empty list runs none. Turn splitting is also returned when needed to build task/session units or identify task-opening turns. */
    @JsonProperty("classifiers") List<String> classifiers,
    /** Maximum concurrent classifier calls in this analysis across all lanes, including segmentation and session classifiers. Defaults to 4; must be positive. Multiple simultaneous analyses each have their own limit. Trajectory concurrency is not inference-request concurrency. */
    @JsonProperty("maxConcurrentRequests") Long maxConcurrentRequests,
    /** Explicit judge configuration for this analysis. Omit to use each classifier's production model policy and reasoning effort. The exact model must be offered by the authenticated catalog, allowed by server policy, and compatible with the classifier; an unusable override fails the affected classification rather than falling back. Choosing a billed model explicitly authorizes its inference costs. Successful rows reused from previousResult keep their original judge configuration. */
    @JsonProperty("judge") TaskAnalyticsJudge judge,
    /** Validate input locally without authentication, model discovery, or inference. Returns validation diagnostics and no classifications. Invalid input never starts paid inference, even when this option is omitted. */
    @JsonProperty("validateOnly") Boolean validateOnly,
    /** Previous result for the identical archive and classifier versions. Reuses all original task boundaries, including unresolved ones, and preserves successful and declined outcomes. Only failed/abandoned non-segmenting outcomes in the selected classifiers are retried. Start a fresh analysis to change segmentation. Rejects mismatched fingerprints or versions before inference. */
    @JsonProperty("previousResult") TaskAnalyticsClassifyTrajectoryResult previousResult
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public TaskAnalyticsClassifyTrajectoryParams {}

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
        private List<?> events;
        private List<String> classifiers;
        private Long maxConcurrentRequests;
        private TaskAnalyticsJudge judge;
        private Boolean validateOnly;
        private TaskAnalyticsClassifyTrajectoryResult previousResult;

        /**
         * Sets the {@code events} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder events(List<?> value) {
            this.events = value;
            return this;
        }

        /**
         * Sets the {@code classifiers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder classifiers(List<String> value) {
            this.classifiers = value;
            return this;
        }

        /**
         * Sets the {@code maxConcurrentRequests} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxConcurrentRequests(Long value) {
            this.maxConcurrentRequests = value;
            return this;
        }

        /**
         * Sets the {@code judge} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder judge(TaskAnalyticsJudge value) {
            this.judge = value;
            return this;
        }

        /**
         * Sets the {@code validateOnly} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder validateOnly(Boolean value) {
            this.validateOnly = value;
            return this;
        }

        /**
         * Sets the {@code previousResult} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder previousResult(TaskAnalyticsClassifyTrajectoryResult value) {
            this.previousResult = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public TaskAnalyticsClassifyTrajectoryParams build() {
            if (events == null) throw new IllegalStateException("Missing required property: events");
            return new TaskAnalyticsClassifyTrajectoryParams(events, classifiers, maxConcurrentRequests, judge, validateOnly, previousResult);
        }
    }
}
