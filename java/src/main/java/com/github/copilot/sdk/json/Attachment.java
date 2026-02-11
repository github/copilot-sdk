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
 * code files, documents, or other relevant content.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var attachment = new Attachment("file", "/path/to/source.java", "Main Source File");
 * }</pre>
 *
 * @param type
 *            the attachment type (e.g., "file")
 * @param path
 *            the absolute path to the file on the filesystem
 * @param displayName
 *            a human-readable display name for the attachment
 * @see MessageOptions#setAttachments(java.util.List)
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Attachment(@JsonProperty("type") String type, @JsonProperty("path") String path,
        @JsonProperty("displayName") String displayName) {
}
