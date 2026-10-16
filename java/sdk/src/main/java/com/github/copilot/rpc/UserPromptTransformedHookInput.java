/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Input for user-prompt-transformed hooks.
 *
 * @param sessionId
 *            the runtime session ID
 * @param timestamp
 *            Unix timestamp in milliseconds
 * @param cwd
 *            the current working directory
 * @param prompt
 *            the prompt after user-prompt-submitted hooks
 * @param transformedPrompt
 *            the model-facing prompt after runtime transformations
 * @since 1.0.11
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
public record UserPromptTransformedHookInput(@JsonProperty("sessionId") String sessionId,
        @JsonProperty("timestamp") long timestamp, @JsonProperty("cwd") String cwd,
        @JsonProperty("prompt") String prompt, @JsonProperty("transformedPrompt") String transformedPrompt,
        @JsonProperty("agentId") String agentId, @JsonProperty("agentType") String agentType,
        @JsonProperty("parentAgentId") String parentAgentId) {
    public UserPromptTransformedHookInput(String sessionId, long timestamp, String cwd, String prompt,
            String transformedPrompt) {
        this(sessionId, timestamp, cwd, prompt, transformedPrompt, null, null, null);
    }
}
