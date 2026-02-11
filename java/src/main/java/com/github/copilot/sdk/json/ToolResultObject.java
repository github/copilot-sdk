/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result object returned from a tool execution.
 * <p>
 * This record represents the structured result of a tool invocation, including
 * text output, binary data, error information, and telemetry.
 *
 * <h2>Example: Success Result</h2>
 *
 * <pre>{@code
 * return ToolResultObject.success("File contents: " + content);
 * }</pre>
 *
 * <h2>Example: Error Result</h2>
 *
 * <pre>{@code
 * return ToolResultObject.error("File not found: " + path);
 * }</pre>
 *
 * <h2>Example: Custom Result</h2>
 *
 * <pre>{@code
 * return new ToolResultObject("success", "Result text", null, null, null, null);
 * }</pre>
 *
 * @param resultType
 *            the result type ("success" or "error"), defaults to "success"
 * @param textResultForLlm
 *            the text result to be sent to the LLM
 * @param binaryResultsForLlm
 *            the list of binary results to be sent to the LLM
 * @param error
 *            the error message, or {@code null} if successful
 * @param sessionLog
 *            the session log text
 * @param toolTelemetry
 *            the tool telemetry data
 * @see ToolHandler
 * @see ToolBinaryResult
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolResultObject(@JsonProperty("resultType") String resultType,
        @JsonProperty("textResultForLlm") String textResultForLlm,
        @JsonProperty("binaryResultsForLlm") List<ToolBinaryResult> binaryResultsForLlm,
        @JsonProperty("error") String error, @JsonProperty("sessionLog") String sessionLog,
        @JsonProperty("toolTelemetry") Map<String, Object> toolTelemetry) {

    /**
     * Creates a success result with the given text.
     *
     * @param textResultForLlm
     *            the text result to be sent to the LLM
     * @return a success result
     */
    public static ToolResultObject success(String textResultForLlm) {
        return new ToolResultObject("success", textResultForLlm, null, null, null, null);
    }

    /**
     * Creates an error result with the given error message.
     *
     * @param error
     *            the error message
     * @return an error result
     */
    public static ToolResultObject error(String error) {
        return new ToolResultObject("error", "An error occurred.", null, error, null, null);
    }

    /**
     * Creates an error result with both a text result and error message.
     *
     * @param textResultForLlm
     *            the text result to be sent to the LLM
     * @param error
     *            the error message
     * @return an error result
     */
    public static ToolResultObject error(String textResultForLlm, String error) {
        return new ToolResultObject("error", textResultForLlm, null, error, null, null);
    }

    /**
     * Creates a failure result with the given text and error message.
     *
     * @param textResultForLlm
     *            the text result to be sent to the LLM
     * @param error
     *            the error message
     * @return a failure result
     */
    public static ToolResultObject failure(String textResultForLlm, String error) {
        return new ToolResultObject("failure", textResultForLlm, null, error, null, null);
    }
}
