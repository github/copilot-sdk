/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Options controlling how Rust-owned built-in tool descriptors are materialized.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionToolsGetBuiltinDescriptorsParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Whether line numbers should be omitted from the view tool descriptor. */
    @JsonProperty("noViewLineNumbers") Boolean noViewLineNumbers,
    /** Whether descriptors should favor fewer user-intervention prompts. */
    @JsonProperty("reduceUserIntervention") Boolean reduceUserIntervention,
    /** Whether tool descriptors should include authoring metadata. */
    @JsonProperty("includeAuthor") Boolean includeAuthor,
    /** Whether semantic skill lookup is available. */
    @JsonProperty("skillEmbeddingEnabled") Boolean skillEmbeddingEnabled,
    /** Shell-specific names and description lines for shell tools. */
    @JsonProperty("shellConfig") ToolsShellDescriptorConfig shellConfig,
    /** Whether shell commands may only run asynchronously. */
    @JsonProperty("shellAsyncOnlyEnabled") Boolean shellAsyncOnlyEnabled,
    /** Whether the configured shell supports PowerShell 7 syntax. */
    @JsonProperty("shellSupportsPowerShell7Syntax") Boolean shellSupportsPowerShell7Syntax,
    /** Default shell timeout in milliseconds. */
    @JsonProperty("shellTimeoutMs") Double shellTimeoutMs,
    /** Whether background task completion notifications are enabled. */
    @JsonProperty("backgroundTaskNotificationsEnabled") Boolean backgroundTaskNotificationsEnabled
) {
}
