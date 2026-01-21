/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Internal request object for sending a message to a session.
 * <p>
 * This is a low-level class for JSON-RPC communication. For sending messages,
 * use {@link com.github.copilot.sdk.CopilotSession#send(String)} or
 * {@link com.github.copilot.sdk.CopilotSession#sendAndWait(String)}.
 *
 * @see com.github.copilot.sdk.CopilotSession
 * @see MessageOptions
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SendMessageRequest {

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("attachments")
    private List<Attachment> attachments;

    @JsonProperty("mode")
    private String mode;

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
    public List<Attachment> getAttachments() {
        return attachments;
    }

    /** Sets the attachments. @param attachments the list of attachments */
    public void setAttachments(List<Attachment> attachments) {
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
}
