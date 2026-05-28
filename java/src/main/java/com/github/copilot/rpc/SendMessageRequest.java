/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Internal request object for sending a message to a session.
 * <p>
 * This is a low-level class for JSON-RPC communication. For sending messages,
 * use {@link com.github.copilot.CopilotSession#send(String)} or
 * {@link com.github.copilot.CopilotSession#sendAndWait(String)}.
 *
 * @see com.github.copilot.CopilotSession
 * @see MessageOptions
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SendMessageRequest {

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("attachments")
    private List<MessageAttachment> attachments;

    @JsonProperty("mode")
    private String mode;

    @JsonProperty("agentMode")
    private AgentMode agentMode;

    @JsonProperty("requestHeaders")
    private Map<String, String> requestHeaders;

    @JsonProperty("displayPrompt")
    private String displayPrompt;

    /** Gets the session ID. @return the session ID */
    public String getSessionId() {
        return sessionId;
    }

    /** Sets the session ID. @param sessionId the session ID */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /** Gets the message prompt. @return the prompt text */
    public String getPrompt() {
        return prompt;
    }

    /** Sets the message prompt. @param prompt the prompt text */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /** Gets the attachments. @return the list of attachments */
    public List<MessageAttachment> getAttachments() {
        return attachments == null ? null : Collections.unmodifiableList(attachments);
    }

    /** Sets the attachments. @param attachments the list of attachments */
    public void setAttachments(List<MessageAttachment> attachments) {
        this.attachments = attachments;
    }

    /** Gets the mode. @return the message mode */
    public String getMode() {
        return mode;
    }

    /** Sets the mode. @param mode the message mode */
    public void setMode(String mode) {
        this.mode = mode;
    }

    /** Gets the per-message agent UI mode. @return the agent mode */
    public AgentMode getAgentMode() {
        return agentMode;
    }

    /** Sets the per-message agent UI mode. @param agentMode the agent mode */
    public void setAgentMode(AgentMode agentMode) {
        this.agentMode = agentMode;
    }

    /** Gets the per-turn request headers. @return the headers map */
    public Map<String, String> getRequestHeaders() {
        return requestHeaders == null ? null : Collections.unmodifiableMap(requestHeaders);
    }

    /** Sets the per-turn request headers. @param requestHeaders the headers map */
    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    /** Gets the display prompt. @return the display prompt */
    public String getDisplayPrompt() {
        return displayPrompt;
    }

    /** Sets the display prompt shown in the timeline instead of the prompt. @param displayPrompt the display prompt */
    public void setDisplayPrompt(String displayPrompt) {
        this.displayPrompt = displayPrompt;
    }
}
