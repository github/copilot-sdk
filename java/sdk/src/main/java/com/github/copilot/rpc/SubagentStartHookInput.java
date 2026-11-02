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
 * @param agentId
 *            optional subagent identity, absent for root hooks and older
 *            runtimes
 * @param agentType
 *            optional subagent identity, absent for root hooks and older
 *            runtimes
 * @param parentAgentId
 *            optional subagent identity, absent for root hooks and older
 *            runtimes
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubagentStartHookInput(@JsonProperty("sessionId") String sessionId,
        @JsonProperty("timestamp") long timestamp, @JsonProperty("cwd") String cwd,
        @JsonProperty("transcriptPath") String transcriptPath, @JsonProperty("agentName") String agentName,
        @JsonProperty("agentDisplayName") String agentDisplayName,
        @JsonProperty("agentDescription") String agentDescription, @JsonProperty("agentId") String agentId,
        @JsonProperty("agentType") String agentType, @JsonProperty("parentAgentId") String parentAgentId) {
    public SubagentStartHookInput(String sessionId, long timestamp, String cwd, String transcriptPath, String agentName,
            String agentDisplayName, String agentDescription) {
        this(sessionId, timestamp, cwd, transcriptPath, agentName, agentDisplayName, agentDescription, null, null,
                null);
    }
}
