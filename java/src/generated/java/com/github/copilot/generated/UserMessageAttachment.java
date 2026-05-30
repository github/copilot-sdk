/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * A user message attachment — a file, directory, code selection, blob, or GitHub reference
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserMessageAttachmentFile.class, name = "file"),
    @JsonSubTypes.Type(value = UserMessageAttachmentDirectory.class, name = "directory"),
    @JsonSubTypes.Type(value = UserMessageAttachmentSelection.class, name = "selection"),
    @JsonSubTypes.Type(value = UserMessageAttachmentGithubReference.class, name = "github_reference"),
    @JsonSubTypes.Type(value = UserMessageAttachmentBlob.class, name = "blob")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class UserMessageAttachment {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the type discriminator
     */
    public abstract String getType();
}
