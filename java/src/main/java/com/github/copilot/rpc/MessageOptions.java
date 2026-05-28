/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * <h2>Blob Attachment Example</h2>
 *
 * <pre>{@code
 * var options = new MessageOptions().setPrompt("Describe this image").setAttachments(List.of(new BlobAttachment()
 * 		.setData("iVBORw0KGgoAAAANSUhEUg...").setMimeType("image/png").setDisplayName("screenshot.png")));
 *
 * session.send(options).get();
 * }</pre>
 *
 * @see com.github.copilot.CopilotSession#send(MessageOptions)
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageOptions {

    private String prompt;
    private List<MessageAttachment> attachments;
    private String mode;
    private AgentMode agentMode;
    private Map<String, String> requestHeaders;
    private String displayPrompt;

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
     * Gets the attachments.
     *
     * @return the list of attachments
     */
    public List<MessageAttachment> getAttachments() {
        return attachments == null ? null : Collections.unmodifiableList(attachments);
    }

    /**
     * Sets attachments to include with the message.
     * <p>
     * Attachments provide additional context to the assistant. Supported types:
     * <ul>
     * <li>{@link Attachment} — file, directory, code selection, or GitHub
     * reference</li>
     * <li>{@link BlobAttachment} — inline base64-encoded binary data (e.g. images)
     * </li>
     * </ul>
     *
     * @param attachments
     *            the list of attachments
     * @return this options instance for method chaining
     * @see Attachment
     * @see BlobAttachment
     */
    public MessageOptions setAttachments(List<? extends MessageAttachment> attachments) {
        this.attachments = attachments != null ? new ArrayList<>(attachments) : null;
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

    /**
     * Sets the per-message agent UI mode.
     * <p>
     * Defaults to the session's current mode when unset.
     *
     * @param agentMode
     *            the agent mode (for example {@link AgentMode#PLAN} or
     *            {@link AgentMode#AUTOPILOT})
     * @return this options instance for method chaining
     */
    public MessageOptions setAgentMode(AgentMode agentMode) {
        this.agentMode = agentMode;
        return this;
    }

    /**
     * Gets the per-message agent UI mode.
     *
     * @return the agent mode, or {@code null} if not set
     */
    public AgentMode getAgentMode() {
        return agentMode;
    }

    /**
     * Gets the custom per-turn HTTP headers for outbound model requests.
     *
     * @return the headers map, or {@code null} if not set
     */
    public Map<String, String> getRequestHeaders() {
        return requestHeaders == null ? null : Collections.unmodifiableMap(requestHeaders);
    }

    /**
     * Sets custom per-turn HTTP headers for outbound model requests.
     * <p>
     * These headers are included in the model API request for this specific message
     * turn. Use this to pass per-request authentication, tracing, or custom
     * metadata.
     *
     * @param requestHeaders
     *            the headers map
     * @return this options instance for method chaining
     */
    public MessageOptions setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
        return this;
    }

    /**
     * Gets the display prompt shown in the timeline instead of the prompt.
     *
     * @return the display prompt, or {@code null} if not set
     */
    public String getDisplayPrompt() {
        return displayPrompt;
    }

    /**
     * Sets the display prompt shown in the timeline instead of the prompt.
     * <p>
     * If provided, this text is displayed in the conversation timeline UI instead
     * of the actual prompt text.
     *
     * @param displayPrompt
     *            the display prompt text
     * @return this options instance for method chaining
     */
    public MessageOptions setDisplayPrompt(String displayPrompt) {
        this.displayPrompt = displayPrompt;
        return this;
    }

    /**
     * Creates a shallow clone of this {@code MessageOptions} instance.
     * <p>
     * Mutable collection properties are copied into new collection instances so
     * that modifications to those collections on the clone do not affect the
     * original. Other reference-type properties (like attachment items) are not
     * deep-cloned; the original and the clone will share those objects.
     *
     * @return a clone of this options instance
     */
    @Override
    public MessageOptions clone() {
        MessageOptions copy = new MessageOptions();
        copy.prompt = this.prompt;
        copy.attachments = this.attachments != null ? new ArrayList<>(this.attachments) : null;
        copy.mode = this.mode;
        copy.agentMode = this.agentMode;
        copy.requestHeaders = this.requestHeaders != null ? new HashMap<>(this.requestHeaders) : null;
        copy.displayPrompt = this.displayPrompt;
        return copy;
    }

}
