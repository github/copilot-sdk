/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Output for a subagent-start hook.
 *
 * @param additionalContext
 *            context prepended to the subagent's initial prompt, or
 *            {@code null}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubagentStartHookOutput(@JsonProperty("additionalContext") String additionalContext) {
}
