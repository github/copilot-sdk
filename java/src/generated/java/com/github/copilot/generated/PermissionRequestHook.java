/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Hook confirmation permission request
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionRequestHook extends PermissionRequest {

    @JsonProperty("kind")
    private final String kind = "hook";

    @Override
    public String getKind() { return kind; }

    /** Tool call ID that triggered this permission request */
    @JsonProperty("toolCallId")
    private String toolCallId;

    /** Name of the tool the hook is gating */
    @JsonProperty("toolName")
    private String toolName;

    /** Arguments of the tool call being gated */
    @JsonProperty("toolArgs")
    private Object toolArgs;

    /** Optional message from the hook explaining why confirmation is needed */
    @JsonProperty("hookMessage")
    private String hookMessage;

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public Object getToolArgs() { return toolArgs; }
    public void setToolArgs(Object toolArgs) { this.toolArgs = toolArgs; }

    public String getHookMessage() { return hookMessage; }
    public void setHookMessage(String hookMessage) { this.hookMessage = hookMessage; }
}
