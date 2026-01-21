/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a file attachment to include with a message.
 * <p>
 * Attachments provide additional context to the AI assistant, such as source
 * code files, documents, or other relevant content. All setter methods return
 * {@code this} for method chaining.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var attachment = new Attachment().setType("file").setPath("/path/to/source.java").setDisplayName("Main Source File");
 * }</pre>
 *
 * @see MessageOptions#setAttachments(java.util.List)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Attachment {

    @JsonProperty("type")
    private String type;

    @JsonProperty("path")
    private String path;

    @JsonProperty("displayName")
    private String displayName;

    /**
     * Gets the attachment type.
     *
     * @return the type (e.g., "file")
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the attachment type.
     * <p>
     * Currently supported types:
     * <ul>
     * <li>"file" - A file from the filesystem</li>
     * </ul>
     *
     * @param type
     *            the attachment type
     * @return this attachment for method chaining
     */
    public Attachment setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Gets the file path.
     *
     * @return the absolute path to the file
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the file path.
     * <p>
     * This should be an absolute path to the file on the filesystem.
     *
     * @param path
     *            the absolute file path
     * @return this attachment for method chaining
     */
    public Attachment setPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * Gets the display name.
     *
     * @return the display name for the attachment
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets a human-readable display name for the attachment.
     * <p>
     * This name is shown to the assistant and may be used when referring to the
     * file in responses.
     *
     * @param displayName
     *            the display name
     * @return this attachment for method chaining
     */
    public Attachment setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }
}
