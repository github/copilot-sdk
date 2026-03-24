/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an inline base64-encoded binary attachment (blob) for messages.
 * <p>
 * Use this attachment type to pass image data or other binary content directly
 * to the assistant, without requiring a file on disk.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var attachment = new BlobAttachment().setData("iVBORw0KGgoAAAANSUhEUg...") // base64-encoded content
 * 		.setMimeType("image/png").setDisplayName("screenshot.png");
 *
 * var options = new MessageOptions().setPrompt("Describe this image").setAttachments(List.of(attachment));
 * }</pre>
 *
 * @see MessageOptions#setAttachments(java.util.List)
 * @since 1.2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class BlobAttachment implements MessageAttachment {

    @JsonProperty("type")
    private final String type = "blob";

    @JsonProperty("data")
    private String data;

    @JsonProperty("mimeType")
    private String mimeType;

    @JsonProperty("displayName")
    private String displayName;

    /**
     * Returns the attachment type, always {@code "blob"}.
     *
     * @return {@code "blob"}
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the base64-encoded binary content.
     *
     * @return the base64 data string
     */
    public String getData() {
        return data;
    }

    /**
     * Sets the base64-encoded binary content.
     *
     * @param data
     *            the base64-encoded content
     * @return this attachment for method chaining
     */
    public BlobAttachment setData(String data) {
        this.data = data;
        return this;
    }

    /**
     * Gets the MIME type of the binary content.
     *
     * @return the MIME type (e.g., {@code "image/png"})
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets the MIME type of the binary content.
     *
     * @param mimeType
     *            the MIME type (e.g., {@code "image/png"}, {@code "image/jpeg"})
     * @return this attachment for method chaining
     */
    public BlobAttachment setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    /**
     * Gets the human-readable display name for the attachment.
     *
     * @return the display name, or {@code null}
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the human-readable display name for the attachment.
     *
     * @param displayName
     *            a user-visible name (e.g., {@code "screenshot.png"})
     * @return this attachment for method chaining
     */
    public BlobAttachment setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }
}
