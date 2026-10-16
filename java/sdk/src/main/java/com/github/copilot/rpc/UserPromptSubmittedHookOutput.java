/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Output for a user-prompt-submitted hook.
 * <p>
 * Allows modifying the user's prompt before processing, or blocking it: a
 * {@code decision} of {@code "block"} refuses the prompt, so no model turn
 * runs, no user message is recorded, and the session emits an ephemeral
 * {@code session.warning} ({@code warningType: "policy_blocked"}) carrying the
 * {@code reason}.
 *
 * @param modifiedPrompt
 *            the modified prompt to use instead of the original, or
 *            {@code null} to use the original
 * @param additionalContext
 *            additional context to be added to the prompt, or {@code null}
 * @param suppressOutput
 *            {@code true} to suppress output, or {@code null}
 * @param decision
 *            {@code "block"} to refuse the prompt, or {@code null}
 * @param reason
 *            why the prompt was blocked, carried by the
 *            {@code session.warning}, or {@code null}
 * @since 1.0.7
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserPromptSubmittedHookOutput(@JsonProperty("modifiedPrompt") String modifiedPrompt,
        @JsonProperty("additionalContext") String additionalContext,
        @JsonProperty("suppressOutput") Boolean suppressOutput, @JsonProperty("decision") String decision,
        @JsonProperty("reason") String reason) {

    /**
     * Creates an output without a block decision.
     * <p>
     * Provided for source and binary compatibility with callers written or compiled
     * before the {@code decision} and {@code reason} components were added.
     * Delegates to the canonical constructor with both set to {@code null}.
     *
     * @param modifiedPrompt
     *            the modified prompt to use instead of the original, or
     *            {@code null} to use the original
     * @param additionalContext
     *            additional context to be added to the prompt, or {@code null}
     * @param suppressOutput
     *            {@code true} to suppress output, or {@code null}
     */
    public UserPromptSubmittedHookOutput(String modifiedPrompt, String additionalContext, Boolean suppressOutput) {
        this(modifiedPrompt, additionalContext, suppressOutput, null, null);
    }

    /**
     * Creates an output that blocks the prompt.
     *
     * @param reason
     *            why the prompt was blocked, carried by the {@code session.warning}
     * @return a new output with decision {@code "block"}
     * @throws NullPointerException
     *             if {@code reason} is {@code null}
     */
    public static UserPromptSubmittedHookOutput block(String reason) {
        return new UserPromptSubmittedHookOutput(null, null, null, "block", Objects.requireNonNull(reason, "reason"));
    }
}
