/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "session.model_change". Model change details including previous and new model identifiers
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionModelChangeEvent extends SessionEvent {

    @Override
    public String getType() { return "session.model_change"; }

    @JsonProperty("data")
    private SessionModelChangeEventData data;

    public SessionModelChangeEventData getData() { return data; }
    public void setData(SessionModelChangeEventData data) { this.data = data; }

    /** Data payload for {@link SessionModelChangeEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionModelChangeEventData(
        /** Model that was previously selected, if any */
        @JsonProperty("previousModel") String previousModel,
        /** Newly selected model identifier */
        @JsonProperty("newModel") String newModel,
        /** Provider selected for newModel. Omitted for legacy or unattributed selections; never inferred from a later selection. */
        @JsonProperty("providerId") String providerId,
        /** Provider of previousModel, when known. A provider-only change is a model selection change even when the model identifiers are equal. */
        @JsonProperty("previousProviderId") String previousProviderId,
        /** Reasoning effort level before the model change, if applicable */
        @JsonProperty("previousReasoningEffort") String previousReasoningEffort,
        /** Reasoning effort level after the model change, if applicable */
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** Model that owns effort embedded in an authored model selection. Omitted for independent reasoning-effort overrides and legacy events. */
        @JsonProperty("reasoningEffortModel") String reasoningEffortModel,
        /** True when the reasoning effort is a managed-policy default bound to reasoningEffortModel. Omitted for agent-authored, user-authored, independent, and legacy effort. */
        @JsonProperty("reasoningEffortManaged") Boolean reasoningEffortManaged,
        /** Reasoning summary mode before the model change, if applicable */
        @JsonProperty("previousReasoningSummary") ReasoningSummary previousReasoningSummary,
        /** Reasoning summary mode after the model change, if applicable */
        @JsonProperty("reasoningSummary") ReasoningSummary reasoningSummary,
        /** Output verbosity level before the model change, if applicable */
        @JsonProperty("previousVerbosity") Verbosity previousVerbosity,
        /** Output verbosity level after the model change, if applicable */
        @JsonProperty("verbosity") Verbosity verbosity,
        /** Context tier after the model change; null explicitly clears a previously selected tier */
        @JsonProperty("contextTier") ContextTier contextTier,
        /** True when contextTier is a managed-policy default. Omitted for user-authored and legacy values. */
        @JsonProperty("contextTierManaged") Boolean contextTierManaged,
        /** Reason the change happened, when not user-initiated. `"rate_limit_auto_switch"` for changes triggered by the auto-mode-switch rate-limit recovery path, or `"refusal_fallback"` when the active model declined a request (content refusal) and the runtime switched to the configured refusal-fallback model. UI clients can use this to render contextual copy. */
        @JsonProperty("cause") String cause,
        /** Origin of the effective model change, when known. */
        @JsonProperty("source") ModelChangeSource source,
        /** Previously committed Auto preference, when one was explicitly selected. */
        @JsonProperty("previousAutoTier") AutoTier previousAutoTier,
        /** Committed Auto preference after the model configuration change, when applicable. */
        @JsonProperty("autoTier") AutoTier autoTier,
        /** True when autoTier is a managed-policy default. Omitted for user-authored and legacy values. */
        @JsonProperty("autoTierManaged") Boolean autoTierManaged
    ) {

        /**
         * Creates event data with the components it had before later optional fields were added.
         */
        public SessionModelChangeEventData(
            String previousModel,
            String newModel,
            String previousReasoningEffort,
            String reasoningEffort,
            String reasoningEffortModel,
            ReasoningSummary previousReasoningSummary,
            ReasoningSummary reasoningSummary,
            Verbosity previousVerbosity,
            Verbosity verbosity,
            ContextTier contextTier,
            String cause,
            ModelChangeSource source,
            AutoTier previousAutoTier,
            AutoTier autoTier
        ) {
            this(previousModel, newModel, null, null, previousReasoningEffort, reasoningEffort, reasoningEffortModel, null, previousReasoningSummary, reasoningSummary, previousVerbosity, verbosity, contextTier, null, cause, source, previousAutoTier, autoTier, null);
        }
    }
}
