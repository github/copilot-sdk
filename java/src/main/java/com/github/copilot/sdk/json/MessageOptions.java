/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Options for sending a message to a Copilot session.
 * <p>
 * This class specifies the message content and optional attachments to send to
 * the assistant. All setter methods return {@code this} for method chaining.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var options = new MessageOptions().setPrompt("Explain this code")
 * 		.setAttachments(List.of(new Attachment("file", "/path/to/file.java", null)));
 *
 * session.send(options).get();
 * }</pre>
 *
 * @see com.github.copilot.sdk.CopilotSession#send(MessageOptions)
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageOptions {

    private String prompt;
    private List<Attachment> attachments;
    private String mode;

    /**
     * Gets the message prompt.
     *
     * @return the prompt text
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Sets the message prompt to send to the assistant.
     *
     * @param prompt
     *            the message text
     * @return this options instance for method chaining
     */
    public MessageOptions setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    /**
     * Gets the file attachments.
     *
     * @return the list of attachments
     */
    public List<Attachment> getAttachments() {
        return attachments == null ? null : Collections.unmodifiableList(attachments);
    }

    /**
     * Sets file attachments to include with the message.
     * <p>
     * Attachments provide additional context to the assistant, such as source code
     * files, documents, or other relevant files.
     *
     * @param attachments
     *            the list of file attachments
     * @return this options instance for method chaining
     * @see Attachment
     */
    public MessageOptions setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
        return this;
    }

    /**
     * Sets the message delivery mode.
     * <p>
     * Valid modes:
     * <ul>
     * <li>"enqueue" - Queue the message for processing (default)</li>
     * <li>"immediate" - Process the message immediately</li>
     * </ul>
     *
     * @param mode
     *            the delivery mode
     * @return this options instance for method chaining
     */
    public MessageOptions setMode(String mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Gets the delivery mode.
     *
     * @return the delivery mode
     */
    public String getMode() {
        return mode;
    }

}
