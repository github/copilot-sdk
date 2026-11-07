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
 * Agent interaction mode to apply to the session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModeSetParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** The session mode the agent is operating in */
    @JsonProperty("mode") SessionMode mode,
    /** Mode the session must currently be in for the change to apply. When set and the session is in a different mode the request is a no-op and reports status 'unchanged'. */
    @JsonProperty("expectedMode") SessionMode expectedMode,
    /** Session whose plan-mode base state should be inherited. */
    @JsonProperty("inheritPlanBaseFromSessionId") String inheritPlanBaseFromSessionId,
    /** Whether a dedicated plan model is configured. */
    @JsonProperty("planModelConfigured") Boolean planModelConfigured,
    /** Dedicated model to use in plan mode, when configured. */
    @JsonProperty("planModel") String planModel,
    /** Provider owning planModel. Omit for legacy bare-model selection. */
    @JsonProperty("planModelProviderId") String planModelProviderId,
    /** Reasoning effort to use with the dedicated plan model. */
    @JsonProperty("planReasoningEffort") String planReasoningEffort,
    /** Context tier to use with the dedicated plan model. */
    @JsonProperty("planContextTier") String planContextTier,
    /** Explicit response to a model-switch compaction preflight. */
    @JsonProperty("compactionDecision") String compactionDecision,
    /** Whether leaving plan mode should restore the session's previous model. */
    @JsonProperty("restorePlanModel") Boolean restorePlanModel,
    /** Whether the selected plan model should be persisted. */
    @JsonProperty("persistPlanSelection") Boolean persistPlanSelection,
    /** Settings context used when persisting the selected plan model. */
    @JsonProperty("pickerSettingsContext") ModelPickerSettingsContext pickerSettingsContext,
    /** Action to perform when leaving plan mode. */
    @JsonProperty("planExitAction") String planExitAction
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionModeSetParams {}

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
        private SessionMode mode;
        private SessionMode expectedMode;
        private String inheritPlanBaseFromSessionId;
        private Boolean planModelConfigured;
        private String planModel;
        private String planModelProviderId;
        private String planReasoningEffort;
        private String planContextTier;
        private String compactionDecision;
        private Boolean restorePlanModel;
        private Boolean persistPlanSelection;
        private ModelPickerSettingsContext pickerSettingsContext;
        private String planExitAction;

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
         * Sets the {@code mode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder mode(SessionMode value) {
            this.mode = value;
            return this;
        }

        /**
         * Sets the {@code expectedMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder expectedMode(SessionMode value) {
            this.expectedMode = value;
            return this;
        }

        /**
         * Sets the {@code inheritPlanBaseFromSessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder inheritPlanBaseFromSessionId(String value) {
            this.inheritPlanBaseFromSessionId = value;
            return this;
        }

        /**
         * Sets the {@code planModelConfigured} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder planModelConfigured(Boolean value) {
            this.planModelConfigured = value;
            return this;
        }

        /**
         * Sets the {@code planModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder planModel(String value) {
            this.planModel = value;
            return this;
        }

        /**
         * Sets the {@code planModelProviderId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder planModelProviderId(String value) {
            this.planModelProviderId = value;
            return this;
        }

        /**
         * Sets the {@code planReasoningEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder planReasoningEffort(String value) {
            this.planReasoningEffort = value;
            return this;
        }

        /**
         * Sets the {@code planContextTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder planContextTier(String value) {
            this.planContextTier = value;
            return this;
        }

        /**
         * Sets the {@code compactionDecision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder compactionDecision(String value) {
            this.compactionDecision = value;
            return this;
        }

        /**
         * Sets the {@code restorePlanModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder restorePlanModel(Boolean value) {
            this.restorePlanModel = value;
            return this;
        }

        /**
         * Sets the {@code persistPlanSelection} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder persistPlanSelection(Boolean value) {
            this.persistPlanSelection = value;
            return this;
        }

        /**
         * Sets the {@code pickerSettingsContext} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder pickerSettingsContext(ModelPickerSettingsContext value) {
            this.pickerSettingsContext = value;
            return this;
        }

        /**
         * Sets the {@code planExitAction} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder planExitAction(String value) {
            this.planExitAction = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionModeSetParams build() {
            if (mode == null) throw new IllegalStateException("Missing required property: mode");
            return new SessionModeSetParams(sessionId, mode, expectedMode, inheritPlanBaseFromSessionId, planModelConfigured, planModel, planModelProviderId, planReasoningEffort, planContextTier, compactionDecision, restorePlanModel, persistPlanSelection, pickerSettingsContext, planExitAction);
        }
    }
}
