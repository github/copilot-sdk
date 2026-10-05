/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Input received after a subagent's turn.
 *
 * @param sessionId
 *            the parent session ID
 * @param timestamp
 *            Unix timestamp in milliseconds
 * @param cwd
 *            the parent session's working directory
 * @param transcriptPath
 *            the parent session transcript path
 * @param agentName
 *            the subagent definition name
 * @param agentType
 *            the subagent type
 * @param agentId
 *            the subagent ID, if available
 * @param agentDisplayName
 *            the optional subagent display name
 * @param agentDescription
 *            the optional subagent description
 * @param stopReason
 *            the reason the subagent stopped
 * @param response
 *            the subagent's last assistant response
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubagentStopHookInput(@JsonProperty("sessionId") String sessionId,
        @JsonProperty("timestamp") long timestamp, @JsonProperty("cwd") String cwd,
        @JsonProperty("transcriptPath") String transcriptPath, @JsonProperty("agentName") String agentName,
        @JsonProperty("agentType") String agentType, @JsonProperty("agentId") String agentId,
        @JsonProperty("agentDisplayName") String agentDisplayName,
        @JsonProperty("agentDescription") String agentDescription, @JsonProperty("stopReason") String stopReason,
        @JsonProperty("response") String response) {
}
