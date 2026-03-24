/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

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
 * @see MessageOptions#setAttachments(java.util.List)
 * @since 1.2.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = Attachment.class, name = "file"),
        @JsonSubTypes.Type(value = BlobAttachment.class, name = "blob")})
public sealed interface MessageAttachment permits Attachment, BlobAttachment {

    /**
     * Returns the attachment type discriminator (e.g., "file", "blob").
     *
     * @return the type string
     */
    String getType();
}
