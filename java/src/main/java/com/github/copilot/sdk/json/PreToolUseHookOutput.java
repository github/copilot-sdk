/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Output for a pre-tool-use hook.
 *
 * @param permissionDecision
 *            "allow", "deny", or "ask"
 * @param permissionDecisionReason
 *            the reason for the permission decision
 * @param modifiedArgs
 *            the modified tool arguments, or {@code null} to use original
 * @param additionalContext
 *            additional context to provide to the model
 * @param suppressOutput
 *            {@code true} to suppress output
 * @since 1.0.6
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PreToolUseHookOutput(@JsonProperty("permissionDecision") String permissionDecision,
        @JsonProperty("permissionDecisionReason") String permissionDecisionReason,
        @JsonProperty("modifiedArgs") JsonNode modifiedArgs,
        @JsonProperty("additionalContext") String additionalContext,
        @JsonProperty("suppressOutput") Boolean suppressOutput) {
}
