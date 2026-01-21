/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Binary result from a tool execution.
 * <p>
 * This class represents binary data (such as images) returned by a tool. The
 * data is base64-encoded for JSON transmission.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var binaryResult = new ToolBinaryResult().setType("image").setMimeType("image/png")
 * 		.setData(Base64.getEncoder().encodeToString(imageBytes)).setDescription("Generated chart");
 * }</pre>
 *
 * @see ToolResultObject#setBinaryResultsForLlm(java.util.List)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ToolBinaryResult {

    @JsonProperty("data")
    private String data;

    @JsonProperty("mimeType")
    private String mimeType;

    @JsonProperty("type")
    private String type;

    @JsonProperty("description")
    private String description;

    /**
     * Gets the base64-encoded binary data.
     *
     * @return the base64-encoded data string
     */
    public String getData() {
        return data;
    }

    /**
     * Sets the base64-encoded binary data.
     *
     * @param data
     *            the base64-encoded data
     * @return this result for method chaining
     */
    public ToolBinaryResult setData(String data) {
        this.data = data;
        return this;
    }

    /**
     * Gets the MIME type of the binary data.
     *
     * @return the MIME type (e.g., "image/png", "application/pdf")
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets the MIME type of the binary data.
     *
     * @param mimeType
     *            the MIME type
     * @return this result for method chaining
     */
    public ToolBinaryResult setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    /**
     * Gets the type of binary content.
     *
     * @return the content type (e.g., "image", "file")
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of binary content.
     *
     * @param type
     *            the content type
     * @return this result for method chaining
     */
    public ToolBinaryResult setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Gets the description of the binary content.
     *
     * @return the content description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets a description of the binary content.
     * <p>
     * This helps the assistant understand the content.
     *
     * @param description
     *            the content description
     * @return this result for method chaining
     */
    public ToolBinaryResult setDescription(String description) {
        this.description = description;
        return this;
    }
}
