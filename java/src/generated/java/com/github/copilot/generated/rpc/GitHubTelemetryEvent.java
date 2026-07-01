/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * A single telemetry event in the runtime's native GitHub-shaped telemetry format, forwarded verbatim to opted-in hosts. The `restricted` flag on the enclosing GitHubTelemetryNotification distinguishes standard from restricted events; the payload shape is identical for both.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubTelemetryEvent(
    /** Event type/kind (e.g. get_completion_with_tools_turn, tool_call_executed). */
    @JsonProperty("kind") String kind,
    /** Timestamp when the event was created (ISO 8601 format). */
    @JsonProperty("created_at") String createdAt,
    /** Reference to the model call that produced this event. */
    @JsonProperty("model_call_id") String modelCallId,
    /** String-valued properties as a map from key to value. */
    @JsonProperty("properties") Map<String, String> properties,
    /** Numeric metrics as a map from key to value. */
    @JsonProperty("metrics") Map<String, Double> metrics,
    /** Experiment assignment context. */
    @JsonProperty("exp_assignment_context") String expAssignmentContext,
    /** Feature flags enabled for this session, as a map from flag to value. */
    @JsonProperty("features") Map<String, String> features,
    /** Session identifier the event belongs to. */
    @JsonProperty("session_id") String sessionId,
    /** Copilot tracking ID for user-level attribution. */
    @JsonProperty("copilot_tracking_id") String copilotTrackingId,
    /** Client environment metadata. */
    @JsonProperty("client") GitHubTelemetryClientInfo client
) {
}
