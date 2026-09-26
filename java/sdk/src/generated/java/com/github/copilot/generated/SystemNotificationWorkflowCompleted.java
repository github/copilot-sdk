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
 * System notification metadata for a workflow execution attempt that reached a terminal state.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SystemNotificationWorkflowCompleted extends SystemNotification {

    @JsonProperty("type")
    private final String type = "workflow_completed";

    @Override
    public String getType() { return type; }

    /** Workflow run identifier. */
    @JsonProperty("runId")
    private String runId;

    /** Persisted workflow name. */
    @JsonProperty("workflowName")
    private String workflowName;

    /** Terminal status reached by this execution attempt. */
    @JsonProperty("status")
    private SystemNotificationWorkflowCompletedStatus status;

    /** Subagents consumed by the run across all attempts. */
    @JsonProperty("consumedSubagents")
    private Long consumedSubagents;

    /** Accumulated active execution time in milliseconds. */
    @JsonProperty("elapsedMs")
    private Long elapsedMs;

    /** Consumed AI usage in nano-AIU. */
    @JsonProperty("consumedNanoAiu")
    private Long consumedNanoAiu;

    /** Execution attempt that reached this terminal state. */
    @JsonProperty("attempt")
    private Long attempt;

    /** Bounded prompt-safe preview of the completed result. */
    @JsonProperty("resultPreview")
    private String resultPreview;

    /** Machine-readable terminal failure details, when present. */
    @JsonProperty("failure")
    private Object failure;

    /** Actionable run_dynamic_workflow resume guidance for a resource-limit failure. */
    @JsonProperty("retryGuidance")
    private String retryGuidance;

    /** Pause initiator metadata when this attempt settled as paused. */
    @JsonProperty("pauseInfo")
    private Object pauseInfo;

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }

    public SystemNotificationWorkflowCompletedStatus getStatus() { return status; }
    public void setStatus(SystemNotificationWorkflowCompletedStatus status) { this.status = status; }

    public Long getConsumedSubagents() { return consumedSubagents; }
    public void setConsumedSubagents(Long consumedSubagents) { this.consumedSubagents = consumedSubagents; }

    public Long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }

    public Long getConsumedNanoAiu() { return consumedNanoAiu; }
    public void setConsumedNanoAiu(Long consumedNanoAiu) { this.consumedNanoAiu = consumedNanoAiu; }

    public Long getAttempt() { return attempt; }
    public void setAttempt(Long attempt) { this.attempt = attempt; }

    public String getResultPreview() { return resultPreview; }
    public void setResultPreview(String resultPreview) { this.resultPreview = resultPreview; }

    public Object getFailure() { return failure; }
    public void setFailure(Object failure) { this.failure = failure; }

    public String getRetryGuidance() { return retryGuidance; }
    public void setRetryGuidance(String retryGuidance) { this.retryGuidance = retryGuidance; }

    public Object getPauseInfo() { return pauseInfo; }
    public void setPauseInfo(Object pauseInfo) { this.pauseInfo = pauseInfo; }
}
