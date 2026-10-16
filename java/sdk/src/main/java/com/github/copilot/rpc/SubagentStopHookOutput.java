/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Output for a subagent-stop hook.
 *
 * @param decision
 *            {@code "block"} to run another subagent turn, or {@code "allow"}
 *            (or {@code null}) to allow the stop; other values fail the
 *            subagent
 * @param reason
 *            the nonempty follow-up instruction required when blocking; invalid
 *            without {@code decision = "block"}
 * @param modifiedResponse
 *            replacement final response when the stop is allowed
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubagentStopHookOutput(@JsonProperty("decision") String decision, @JsonProperty("reason") String reason,
        @JsonProperty("modifiedResponse") String modifiedResponse) {
}
