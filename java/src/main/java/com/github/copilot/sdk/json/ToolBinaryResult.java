/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Binary result from a tool execution.
 * <p>
 * This record represents binary data (such as images) returned by a tool. The
 * data is base64-encoded for JSON transmission.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var binaryResult = new ToolBinaryResult(Base64.getEncoder().encodeToString(imageBytes), "image/png", "image",
 * 		"Generated chart");
 * }</pre>
 *
 * @param data
 *            the base64-encoded binary data
 * @param mimeType
 *            the MIME type (e.g., "image/png", "application/pdf")
 * @param type
 *            the content type (e.g., "image", "file")
 * @param description
 *            the content description, helps the assistant understand the
 *            content
 * @see ToolResultObject#setBinaryResultsForLlm(java.util.List)
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolBinaryResult(@JsonProperty("data") String data, @JsonProperty("mimeType") String mimeType,
        @JsonProperty("type") String type, @JsonProperty("description") String description) {
}
