/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Map;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents a permission request from the AI assistant.
 * <p>
 * When the assistant needs permission to perform certain actions, this object
 * contains the details of the request, including the kind of permission and any
 * associated tool call.
 *
 * @see PermissionHandler
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PermissionRequest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("toolCallId")
    private String toolCallId;

    @JsonProperty("managedApprovalRequired")
    private Boolean managedApprovalRequired;

    private Map<String, Object> extensionData;

    /**
     * Converts the value exposed by a {@code permission.requested} event into a
     * typed permission request.
     *
     * @param value
     *            the event's {@code permissionRequest} value
     * @return the typed permission request
     * @throws IllegalArgumentException
     *             if the value cannot be converted
     */
    public static PermissionRequest fromJsonValue(Object value) {
        if (value instanceof PermissionRequest request) {
            return request;
        }
        return MAPPER.convertValue(value, PermissionRequest.class);
    }

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
     * Gets whether managed policy requires an explicit human decision.
     *
     * @return {@code true} when automatic approval must be bypassed, otherwise
     *         {@code false} or {@code null}
     */
    public Boolean getManagedApprovalRequired() {
        return managedApprovalRequired;
    }

    /**
     * Sets whether managed policy requires an explicit human decision.
     *
     * @param managedApprovalRequired
     *            whether managed approval is required
     */
    public void setManagedApprovalRequired(Boolean managedApprovalRequired) {
        this.managedApprovalRequired = managedApprovalRequired;
    }

    /**
     * Gets additional extension data for the request.
     *
     * @return the extension data map
     */
    @JsonAnyGetter
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

    /**
     * Captures variant-specific permission request fields.
     *
     * @param name
     *            the JSON property name
     * @param value
     *            the JSON property value
     */
    @JsonAnySetter
    public void setExtensionData(String name, Object value) {
        if (extensionData == null) {
            extensionData = new HashMap<>();
        }
        extensionData.put(name, value);
    }
}
