/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Expanded canonical result returned by a session tool.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolResultExpanded(
    /** Text result returned to the model. */
    @JsonProperty("textResultForLlm") String textResultForLlm,
    /** Execution outcome classification. */
    @JsonProperty("resultType") ToolResultType resultType,
    /** Base64-encoded binary results returned to the model. */
    @JsonProperty("binaryResultsForLlm") List<ExternalToolTextResultForLlmBinaryResultsForLlm> binaryResultsForLlm,
    /** Detailed log content available for session display. */
    @JsonProperty("sessionLog") String sessionLog,
    /** Error message for an unsuccessful execution. */
    @JsonProperty("error") String error,
    /** Tool-specific telemetry payload. */
    @JsonProperty("toolTelemetry") Object toolTelemetry,
    /** Whether large-output post-processing should be skipped. */
    @JsonProperty("skipLargeOutputProcessing") Boolean skipLargeOutputProcessing,
    /** Messages to inject after the tool result. */
    @JsonProperty("newMessages") List<ToolResultNewMessage> newMessages,
    /** Structured content blocks returned to the model. */
    @JsonProperty("contents") List<Object> contents,
    /** Deferred tool names made available by this result. */
    @JsonProperty("toolReferences") List<String> toolReferences,
    /** Sources returned by the tool that the model may cite. */
    @JsonProperty("citableSources") List<Object> citableSources,
    /** Skill invocation metadata produced by the tool. */
    @JsonProperty("skillInvocation") Object skillInvocation,
    /** Whether post-tool-use failure hooks have already processed this result. */
    @JsonProperty("postToolUseFailureHooksProcessed") Boolean postToolUseFailureHooksProcessed,
    /** Optional UI resource produced by the tool. */
    @JsonProperty("uiResource") Object uiResource,
    /** Metadata propagated with the tool result, including information-flow labels. */
    @JsonProperty("mcpMeta") Map<String, Object> mcpMeta,
    /** Structured result content in addition to the model-facing text. */
    @JsonProperty("structuredContent") Object structuredContent,
    /** Completion-review decision produced by the task-completion tool. */
    @JsonProperty("taskCompletionDecision") TaskCompletionDecision taskCompletionDecision
) {
}
