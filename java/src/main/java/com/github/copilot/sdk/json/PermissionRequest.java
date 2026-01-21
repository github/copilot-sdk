/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a permission request from the AI assistant.
 * <p>
 * When the assistant needs permission to perform certain actions, this object
 * contains the details of the request, including the kind of permission and any
 * associated tool call.
 *
 * @see PermissionHandler
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PermissionRequest {

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("toolCallId")
    private String toolCallId;

    private Map<String, Object> extensionData;

    /**
     * Gets the kind of permission being requested.
     *
     * @return the permission kind
     */
    public String getKind() {
        return kind;
    }

    /**
     * Sets the permission kind.
     *
     * @param kind
     *            the permission kind
     */
    public void setKind(String kind) {
        this.kind = kind;
    }

    /**
     * Gets the associated tool call ID, if applicable.
     *
     * @return the tool call ID, or {@code null} if not a tool-related request
     */
    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * Sets the tool call ID.
     *
     * @param toolCallId
     *            the tool call ID
     */
    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    /**
     * Gets additional extension data for the request.
     *
     * @return the extension data map
     */
    public Map<String, Object> getExtensionData() {
        return extensionData;
    }

    /**
     * Sets additional extension data for the request.
     *
     * @param extensionData
     *            the extension data map
     */
    public void setExtensionData(Map<String, Object> extensionData) {
        this.extensionData = extensionData;
    }
}
