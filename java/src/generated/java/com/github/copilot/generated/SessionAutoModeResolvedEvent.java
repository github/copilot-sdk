/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "session.auto_mode_resolved". Auto Intent resolution: the concrete model the session settled on for the first prompt of an auto-mode session, and why. Lets SDK clients render the chosen model and the full reason it was picked. The core selection fields (chosenModel/reasoningBucket/categoryScores) are stable; the routing-analytics fields (predictedLabel/confidence/candidateModels) mirror the upstream intent service and may evolve, hence the event's experimental stability.
 * @since 1.0.0
 */
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
        @JsonProperty("candidateModels") List<String> candidateModels
    ) {
    }
}
