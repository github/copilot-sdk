/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Structured context contributed by an extension.
 *
 * @see MessageOptions#setAttachments(java.util.List)
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ExtensionContextAttachment implements MessageAttachment {

    @JsonProperty("type")
    private final String type = "extension_context";

    @JsonProperty("capturedAt")
    private String capturedAt;

    @JsonProperty("extensionId")
    private String extensionId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("canvasId")
    private String canvasId;

    @JsonProperty("instanceId")
    private String instanceId;

    @JsonProperty("payload")
    private Object payload;

    /**
     * Returns the attachment type, always {@code "extension_context"}.
     *
     * @return {@code "extension_context"}
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * Gets the ISO 8601 capture timestamp.
     *
     * @return the capture timestamp
     */
    public String getCapturedAt() {
        return capturedAt;
    }

    /**
     * Sets the ISO 8601 capture timestamp.
     *
     * @param capturedAt
     *            the capture timestamp
     * @return this attachment for method chaining
     */
    public ExtensionContextAttachment setCapturedAt(String capturedAt) {
        this.capturedAt = capturedAt;
        return this;
    }

    /**
     * Gets the owning extension identifier.
     *
     * @return the extension identifier
     */
    public String getExtensionId() {
        return extensionId;
    }

    /**
     * Sets the owning extension identifier.
     *
     * @param extensionId
     *            the extension identifier
     * @return this attachment for method chaining
     */
    public ExtensionContextAttachment setExtensionId(String extensionId) {
        this.extensionId = extensionId;
        return this;
    }

    /**
     * Gets the human-readable context title.
     *
     * @return the context title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the human-readable context title.
     *
     * @param title
     *            the context title
     * @return this attachment for method chaining
     */
    public ExtensionContextAttachment setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Gets the provider-local canvas identifier.
     *
     * @return the canvas identifier, or {@code null}
     */
    public String getCanvasId() {
        return canvasId;
    }

    /**
     * Sets the provider-local canvas identifier.
     *
     * @param canvasId
     *            the canvas identifier
     * @return this attachment for method chaining
     */
    public ExtensionContextAttachment setCanvasId(String canvasId) {
        this.canvasId = canvasId;
        return this;
    }

    /**
     * Gets the open canvas instance identifier.
     *
     * @return the instance identifier, or {@code null}
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets the open canvas instance identifier.
     *
     * @param instanceId
     *            the instance identifier
     * @return this attachment for method chaining
     */
    public ExtensionContextAttachment setInstanceId(String instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    /**
     * Gets the extension-defined structured payload.
     *
     * @return the structured payload, or {@code null}
     */
    public Object getPayload() {
        return payload;
    }

    /**
     * Sets the extension-defined structured payload.
     *
     * @param payload
     *            a JSON-serializable payload
     * @return this attachment for method chaining
     */
    public ExtensionContextAttachment setPayload(Object payload) {
        this.payload = payload;
        return this;
    }
}
