/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "session.auto_mode_resolved". Auto Intent resolution: the concrete model the session settled on for the first prompt of an auto-mode session, and why. Lets SDK clients render the chosen model and the full reason it was picked. The core selection fields (chosenModel/reasoningBucket/categoryScores) are stable; the routing-analytics fields (predictedLabel/confidence/candidateModels) mirror the upstream intent service and may evolve, hence the event's experimental stability.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionAutoModeResolvedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.auto_mode_resolved"; }

    @JsonProperty("data")
    private SessionAutoModeResolvedEventData data;

    public SessionAutoModeResolvedEventData getData() { return data; }
    public void setData(SessionAutoModeResolvedEventData data) { this.data = data; }

    /** Data payload for {@link SessionAutoModeResolvedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionAutoModeResolvedEventData(
        /** The concrete model the session will use after any intent refinement */
        @JsonProperty("chosenModel") String chosenModel,
        /** Coarse request-difficulty bucket, for explaining why a model was chosen ("picked X because this looks like high-reasoning work") */
        @JsonProperty("reasoningBucket") AutoModeResolvedReasoningBucket reasoningBucket,
        /** Per-category classifier scores (0-1) behind the bucket: the granular HYDRA capability scores (reasoning, code_gen, debugging, tool_use), or the binary needs_reasoning/no_reasoning scores when HYDRA didn't run. Lets clients show a breakdown rather than just the bucket. */
        @JsonProperty("categoryScores") Map<String, Double> categoryScores,
        /** The predicted classifier label (e.g. `needs_reasoning`), when available */
        @JsonProperty("predictedLabel") String predictedLabel,
        /** Classifier confidence for the predicted label, when available */
        @JsonProperty("confidence") Double confidence,
        /** Ordered candidate model list the router returned, when not a fallback */
        @JsonProperty("candidateModels") List<String> candidateModels,
        /** The routing method the server applied, when Auto Intent ran */
        @JsonProperty("routingMethod") String routingMethod,
        /** Models offered to the router for this resolution */
        @JsonProperty("availableModels") List<String> availableModels,
        /** Whether the router fell back to the standard Auto selection */
        @JsonProperty("fallback") Boolean fallback,
        /** Server-provided reason for falling back, when available */
        @JsonProperty("fallbackReason") String fallbackReason,
        /** Short human-readable sentence from the routing service explaining why this model was chosen, for display alongside the model. Present only when the service supplied one: it is omitted for on-device selections, when the service did not provide an explanation, and when a replayed decision made no routing call. The text is display-only and drawn from a fixed catalogue; several distinct routing categories share identical wording, so it cannot be used to recover the category or keyed on programmatically. */
        @JsonProperty("selectionReason") String selectionReason,
        /** Whether a sticky model choice overrode the router result */
        @JsonProperty("stickyOverride") Boolean stickyOverride,
        /** Server-reported router processing time in milliseconds */
        @JsonProperty("routerLatencyMs") Double routerLatencyMs,
        /** End-to-end client wait time for the router request in milliseconds */
        @JsonProperty("endToEndLatencyMs") Double endToEndLatencyMs,
        /** The chosen model's score shortfall relative to the top candidate */
        @JsonProperty("chosenShortfall") Double chosenShortfall,
        /** Whether the routed prompt contained an image */
        @JsonProperty("hasImage") Boolean hasImage
    ) {
    }
}
