/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Output for a user-prompt-submitted hook.
 * <p>
 * Allows modifying the user's prompt before processing.
 *
 * @param modifiedPrompt
 *            the modified prompt to use instead of the original, or
 *            {@code null} to use the original
 * @param additionalContext
 *            additional context to be added to the prompt, or {@code null}
 * @param suppressOutput
 *            {@code true} to suppress output, or {@code null}
 * @since 1.0.7
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserPromptSubmittedHookOutput(@JsonProperty("modifiedPrompt") String modifiedPrompt,
        @JsonProperty("additionalContext") String additionalContext,
        @JsonProperty("suppressOutput") Boolean suppressOutput) {
}
