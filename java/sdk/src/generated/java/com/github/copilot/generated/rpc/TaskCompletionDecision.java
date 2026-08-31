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

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskCompletionDecision(
    /** Semantic result of evaluating the task completion request. */
    @JsonProperty("outcome") TaskCompletionOutcome outcome,
    /** Rationale for the completion decision, when one is available. */
    @JsonProperty("reason") String reason,
    /** Whether the rationale was derived from completion-reviewer output. */
    @JsonProperty("reviewerDerived") Boolean reviewerDerived,
    /** Information-flow metadata captured from the completion reviewer. */
    @JsonProperty("reviewerResultMeta") Object reviewerResultMeta,
    /** Active autopilot objective evaluated by the completion reviewer. */
    @JsonProperty("objectiveId") Long objectiveId,
    /** Whether completion was accepted after the reviewer-rejection budget was exhausted. */
    @JsonProperty("completionRejectionBudgetExhausted") Boolean completionRejectionBudgetExhausted,
    /** Objective eligibility token captured when the decision was evaluated. */
    @JsonProperty("completionEligibilityToken") Long completionEligibilityToken
) {
}
