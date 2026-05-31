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
 * Memory operation permission prompt
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionPromptRequestMemory extends PermissionPromptRequest {

    @JsonProperty("kind")
    private final String kind = "memory";

    @Override
    public String getKind() { return kind; }

    /** Tool call ID that triggered this permission request */
    @JsonProperty("toolCallId")
    private String toolCallId;

    /** Whether this is a store or vote memory operation */
    @JsonProperty("action")
    private PermissionRequestMemoryAction action;

    /** Topic or subject of the memory (store only) */
    @JsonProperty("subject")
    private String subject;

    /** The fact being stored or voted on */
    @JsonProperty("fact")
    private String fact;

    /** Source references for the stored fact (store only) */
    @JsonProperty("citations")
    private String citations;

    /** Vote direction (vote only) */
    @JsonProperty("direction")
    private PermissionRequestMemoryDirection direction;

    /** Reason for the vote (vote only) */
    @JsonProperty("reason")
    private String reason;

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    public PermissionRequestMemoryAction getAction() { return action; }
    public void setAction(PermissionRequestMemoryAction action) { this.action = action; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getFact() { return fact; }
    public void setFact(String fact) { this.fact = fact; }

    public String getCitations() { return citations; }
    public void setCitations(String citations) { this.citations = citations; }

    public PermissionRequestMemoryDirection getDirection() { return direction; }
    public void setDirection(PermissionRequestMemoryDirection direction) { this.direction = direction; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
