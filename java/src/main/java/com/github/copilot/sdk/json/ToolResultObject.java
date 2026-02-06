/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result object returned from a tool execution.
 * <p>
 * This class represents the structured result of a tool invocation, including
 * text output, binary data, error information, and telemetry.
 *
 * <h2>Example: Success Result</h2>
 *
 * <pre>{@code
 * return new ToolResultObject().setResultType("success").setTextResultForLlm("File contents: " + content);
 * }</pre>
 *
 * <h2>Example: Error Result</h2>
 *
 * <pre>{@code
 * return new ToolResultObject().setResultType("error").setError("File not found: " + path);
 * }</pre>
 *
 * @see ToolHandler
 * @see ToolBinaryResult
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ToolResultObject {

    @JsonProperty("textResultForLlm")
    private String textResultForLlm;

    @JsonProperty("binaryResultsForLlm")
    private List<ToolBinaryResult> binaryResultsForLlm;

    @JsonProperty("resultType")
    private String resultType = "success";

    @JsonProperty("error")
    private String error;

    @JsonProperty("sessionLog")
    private String sessionLog;

    @JsonProperty("toolTelemetry")
    private Map<String, Object> toolTelemetry;

    /**
     * Gets the text result to be sent to the LLM.
     *
     * @return the text result
     */
    public String getTextResultForLlm() {
        return textResultForLlm;
    }

    /**
     * Sets the text result to be sent to the LLM.
     *
     * @param textResultForLlm
     *            the text result
     * @return this result for method chaining
     */
    public ToolResultObject setTextResultForLlm(String textResultForLlm) {
        this.textResultForLlm = textResultForLlm;
        return this;
    }

    /**
     * Gets the binary results to be sent to the LLM.
     *
     * @return the list of binary results
     */
    public List<ToolBinaryResult> getBinaryResultsForLlm() {
        return binaryResultsForLlm == null ? null : Collections.unmodifiableList(binaryResultsForLlm);
    }

    /**
     * Sets binary results (images, files) to be sent to the LLM.
     *
     * @param binaryResultsForLlm
     *            the list of binary results
     * @return this result for method chaining
     */
    public ToolResultObject setBinaryResultsForLlm(List<ToolBinaryResult> binaryResultsForLlm) {
        this.binaryResultsForLlm = binaryResultsForLlm;
        return this;
    }

    /**
     * Gets the result type.
     *
     * @return the result type ("success" or "error")
     */
    public String getResultType() {
        return resultType;
    }

    /**
     * Sets the result type.
     *
     * @param resultType
     *            "success" or "error"
     * @return this result for method chaining
     */
    public ToolResultObject setResultType(String resultType) {
        this.resultType = resultType;
        return this;
    }

    /**
     * Gets the error message.
     *
     * @return the error message, or {@code null} if successful
     */
    public String getError() {
        return error;
    }

    /**
     * Sets an error message for failed tool execution.
     *
     * @param error
     *            the error message
     * @return this result for method chaining
     */
    public ToolResultObject setError(String error) {
        this.error = error;
        return this;
    }

    /**
     * Gets the session log entry.
     *
     * @return the session log text
     */
    public String getSessionLog() {
        return sessionLog;
    }

    /**
     * Sets a log entry to be recorded in the session.
     *
     * @param sessionLog
     *            the log entry
     * @return this result for method chaining
     */
    public ToolResultObject setSessionLog(String sessionLog) {
        this.sessionLog = sessionLog;
        return this;
    }

    /**
     * Gets the tool telemetry data.
     *
     * @return the telemetry map
     */
    public Map<String, Object> getToolTelemetry() {
        return toolTelemetry == null ? null : Collections.unmodifiableMap(toolTelemetry);
    }

    public ToolResultObject setToolTelemetry(Map<String, Object> toolTelemetry) {
        this.toolTelemetry = toolTelemetry;
        return this;
    }
}
