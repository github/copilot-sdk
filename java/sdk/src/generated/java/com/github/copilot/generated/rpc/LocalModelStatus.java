/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Effective file or download state.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = LocalModelStatusNotDownloaded.class, name = "not-downloaded"),
    @JsonSubTypes.Type(value = LocalModelStatusDownloaded.class, name = "downloaded"),
    @JsonSubTypes.Type(value = LocalModelStatusDownloading.class, name = "downloading")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class LocalModelStatus {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
