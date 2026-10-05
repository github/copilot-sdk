/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Input received before a subagent's first turn.
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
 * @param agentDisplayName
 *            the optional subagent display name
 * @param agentDescription
 *            the optional subagent description
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubagentStartHookInput(@JsonProperty("sessionId") String sessionId,
        @JsonProperty("timestamp") long timestamp, @JsonProperty("cwd") String cwd,
        @JsonProperty("transcriptPath") String transcriptPath, @JsonProperty("agentName") String agentName,
        @JsonProperty("agentDisplayName") String agentDisplayName,
        @JsonProperty("agentDescription") String agentDescription) {
}
