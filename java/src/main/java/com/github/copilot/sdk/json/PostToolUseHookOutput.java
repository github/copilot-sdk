/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Output for a post-tool-use hook.
 *
 * @param modifiedResult
 *            the modified tool result, or {@code null} to use original
 * @param additionalContext
 *            additional context to provide to the model
 * @param suppressOutput
 *            {@code true} to suppress output
 * @since 1.0.6
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostToolUseHookOutput(@JsonProperty("modifiedResult") JsonNode modifiedResult,
        @JsonProperty("additionalContext") String additionalContext,
        @JsonProperty("suppressOutput") Boolean suppressOutput) {
}
