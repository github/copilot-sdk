/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Marker interface for all attachment types that can be included in a message.
 * <p>
 * This is the Java equivalent of the .NET SDK's
 * {@code UserMessageDataAttachmentsItem} polymorphic base class.
 *
 * @see Attachment
 * @see BlobAttachment
 * @see ExtensionContextAttachment
 * @see MessageOptions#setAttachments(java.util.List)
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = Attachment.class, name = "file"),
        @JsonSubTypes.Type(value = BlobAttachment.class, name = "blob"),
        @JsonSubTypes.Type(value = ExtensionContextAttachment.class, name = "extension_context")})
public sealed interface MessageAttachment permits Attachment, BlobAttachment, ExtensionContextAttachment {

    /**
     * Returns the attachment type discriminator (e.g., "file", "blob").
     *
     * @return the type string
     */
    String getType();
}
