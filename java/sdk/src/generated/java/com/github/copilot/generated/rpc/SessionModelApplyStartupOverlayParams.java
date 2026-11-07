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
 * Managed, repository, and CLI model overrides to overlay onto the session at startup.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionModelApplyStartupOverlayParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Model required by device-managed policy, when configured. */
    @JsonProperty("deviceManagedModel") String deviceManagedModel,
    /** Model required by server-managed policy, when configured. */
    @JsonProperty("serverManagedModel") String serverManagedModel,
    /** Startup default model from the enterprise policy helper, when configured. Weakest of the managed sources: it applies only when neither device nor server policy names a model, and an explicit user selection still wins. */
    @JsonProperty("policyHelperModel") String policyHelperModel,
    /** Effective default Auto routing preference from user and managed settings. Applies only to fresh sessions and never replaces a per-session selection. */
    @JsonProperty("autoTier") AutoTier autoTier,
    /** Model selected by repository settings, when configured. */
    @JsonProperty("repoModel") String repoModel,
    /** Provider owning the repository-selected model. Omitted repository provider identity retains legacy bare-model behavior. */
    @JsonProperty("repoModelProviderId") String repoModelProviderId,
    /** Reasoning effort selected by repository settings, when configured. */
    @JsonProperty("repoReasoningEffort") String repoReasoningEffort,
    /** Context tier selected by repository settings, when configured. */
    @JsonProperty("repoContextTier") String repoContextTier,
    /** Auto routing preference selected by repository settings, when configured. Applied only when the overlay selects the Auto model; beside a concrete model it stays dormant. */
    @JsonProperty("repoAutoTier") String repoAutoTier,
    /** Model explicitly selected by the CLI, when provided. */
    @JsonProperty("cliModel") String cliModel,
    /** Whether the overlay is being applied while resuming a deferred session. */
    @JsonProperty("deferredResume") Boolean deferredResume,
    /** Reasoning effort paired with the effective organization-managed model. Applies only when that concrete managed model is selected; it is ignored for Auto and for CLI, resume, or user overrides. */
    @JsonProperty("managedReasoningEffort") String managedReasoningEffort,
    /** Context tier paired with the effective organization-managed model. Applies only when that concrete managed model is selected; it is ignored for Auto and for CLI, resume, or user overrides. */
    @JsonProperty("managedContextTier") String managedContextTier
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionModelApplyStartupOverlayParams {}

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
        private String sessionId;
        private String deviceManagedModel;
        private String serverManagedModel;
        private String policyHelperModel;
        private AutoTier autoTier;
        private String repoModel;
        private String repoModelProviderId;
        private String repoReasoningEffort;
        private String repoContextTier;
        private String repoAutoTier;
        private String cliModel;
        private Boolean deferredResume;
        private String managedReasoningEffort;
        private String managedContextTier;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code deviceManagedModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder deviceManagedModel(String value) {
            this.deviceManagedModel = value;
            return this;
        }

        /**
         * Sets the {@code serverManagedModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder serverManagedModel(String value) {
            this.serverManagedModel = value;
            return this;
        }

        /**
         * Sets the {@code policyHelperModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder policyHelperModel(String value) {
            this.policyHelperModel = value;
            return this;
        }

        /**
         * Sets the {@code autoTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder autoTier(AutoTier value) {
            this.autoTier = value;
            return this;
        }

        /**
         * Sets the {@code repoModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder repoModel(String value) {
            this.repoModel = value;
            return this;
        }

        /**
         * Sets the {@code repoModelProviderId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder repoModelProviderId(String value) {
            this.repoModelProviderId = value;
            return this;
        }

        /**
         * Sets the {@code repoReasoningEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder repoReasoningEffort(String value) {
            this.repoReasoningEffort = value;
            return this;
        }

        /**
         * Sets the {@code repoContextTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder repoContextTier(String value) {
            this.repoContextTier = value;
            return this;
        }

        /**
         * Sets the {@code repoAutoTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder repoAutoTier(String value) {
            this.repoAutoTier = value;
            return this;
        }

        /**
         * Sets the {@code cliModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder cliModel(String value) {
            this.cliModel = value;
            return this;
        }

        /**
         * Sets the {@code deferredResume} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder deferredResume(Boolean value) {
            this.deferredResume = value;
            return this;
        }

        /**
         * Sets the {@code managedReasoningEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder managedReasoningEffort(String value) {
            this.managedReasoningEffort = value;
            return this;
        }

        /**
         * Sets the {@code managedContextTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder managedContextTier(String value) {
            this.managedContextTier = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionModelApplyStartupOverlayParams build() {
            return new SessionModelApplyStartupOverlayParams(sessionId, deviceManagedModel, serverManagedModel, policyHelperModel, autoTier, repoModel, repoModelProviderId, repoReasoningEffort, repoContextTier, repoAutoTier, cliModel, deferredResume, managedReasoningEffort, managedContextTier);
        }
    }
}
