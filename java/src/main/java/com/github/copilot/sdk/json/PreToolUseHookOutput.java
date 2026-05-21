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

    /**
     * Creates an output that allows the tool to execute.
     *
     * @return a new PreToolUseHookOutput with permission decision "allow"
     */
    public static PreToolUseHookOutput allow() {
        return new PreToolUseHookOutput("allow", null, null, null, null);
    }

    /**
     * Creates an output that denies the tool execution.
     *
     * @return a new PreToolUseHookOutput with permission decision "deny"
     */
    public static PreToolUseHookOutput deny() {
        return new PreToolUseHookOutput("deny", null, null, null, null);
    }

    /**
     * Creates an output that denies the tool execution with a reason.
     *
     * @param reason
     *            the reason for denying the tool execution
     * @return a new PreToolUseHookOutput with permission decision "deny" and reason
     */
    public static PreToolUseHookOutput deny(String reason) {
        return new PreToolUseHookOutput("deny", reason, null, null, null);
    }

    /**
     * Creates an output that asks for user confirmation before executing the tool.
     *
     * @return a new PreToolUseHookOutput with permission decision "ask"
     */
    public static PreToolUseHookOutput ask() {
        return new PreToolUseHookOutput("ask", null, null, null, null);
    }

    /**
     * Creates an output with modified tool arguments.
     *
     * @param permissionDecision
     *            "allow", "deny", or "ask"
     * @param modifiedArgs
     *            the modified tool arguments
     * @return a new PreToolUseHookOutput with the specified permission and modified
     *         arguments
     */
    public static PreToolUseHookOutput withModifiedArgs(String permissionDecision, JsonNode modifiedArgs) {
        return new PreToolUseHookOutput(permissionDecision, null, modifiedArgs, null, null);
    }
}
