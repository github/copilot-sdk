/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Output for user-prompt-transformed hooks.
 *
 * @param modifiedTransformedPrompt
 *            replacement model-facing prompt to persist and send to the model
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserPromptTransformedHookOutput(
        @JsonProperty("modifiedTransformedPrompt") String modifiedTransformedPrompt) {
}
