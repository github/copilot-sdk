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
 * @since 1.0.6
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreToolUseHookOutput {

    @JsonProperty("permissionDecision")
    private String permissionDecision;

    @JsonProperty("permissionDecisionReason")
    private String permissionDecisionReason;

    @JsonProperty("modifiedArgs")
    private JsonNode modifiedArgs;

    @JsonProperty("additionalContext")
    private String additionalContext;

    @JsonProperty("suppressOutput")
    private Boolean suppressOutput;

    /**
     * Gets the permission decision.
     *
     * @return "allow", "deny", or "ask"
     */
    public String getPermissionDecision() {
        return permissionDecision;
    }

    /**
     * Sets the permission decision.
     *
     * @param permissionDecision
     *            "allow", "deny", or "ask"
     * @return this instance for method chaining
     */
    public PreToolUseHookOutput setPermissionDecision(String permissionDecision) {
        this.permissionDecision = permissionDecision;
        return this;
    }

    /**
     * Gets the reason for the permission decision.
     *
     * @return the reason text
     */
    public String getPermissionDecisionReason() {
        return permissionDecisionReason;
    }

    /**
     * Sets the reason for the permission decision.
     *
     * @param permissionDecisionReason
     *            the reason text
     * @return this instance for method chaining
     */
    public PreToolUseHookOutput setPermissionDecisionReason(String permissionDecisionReason) {
        this.permissionDecisionReason = permissionDecisionReason;
        return this;
    }

    /**
     * Gets the modified tool arguments.
     *
     * @return the modified arguments, or {@code null} to use original
     */
    public JsonNode getModifiedArgs() {
        return modifiedArgs;
    }

    /**
     * Sets the modified tool arguments.
     *
     * @param modifiedArgs
     *            the modified arguments
     * @return this instance for method chaining
     */
    public PreToolUseHookOutput setModifiedArgs(JsonNode modifiedArgs) {
        this.modifiedArgs = modifiedArgs;
        return this;
    }

    /**
     * Gets additional context to provide to the model.
     *
     * @return the additional context
     */
    public String getAdditionalContext() {
        return additionalContext;
    }

    /**
     * Sets additional context to provide to the model.
     *
     * @param additionalContext
     *            the additional context
     * @return this instance for method chaining
     */
    public PreToolUseHookOutput setAdditionalContext(String additionalContext) {
        this.additionalContext = additionalContext;
        return this;
    }

    /**
     * Returns whether to suppress output.
     *
     * @return {@code true} to suppress output
     */
    public Boolean getSuppressOutput() {
        return suppressOutput;
    }

    /**
     * Sets whether to suppress output.
     *
     * @param suppressOutput
     *            {@code true} to suppress output
     * @return this instance for method chaining
     */
    public PreToolUseHookOutput setSuppressOutput(Boolean suppressOutput) {
        this.suppressOutput = suppressOutput;
        return this;
    }
}
