/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * HTTP response metadata and the selected body representation. HTTP error statuses remain successful transport outcomes.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "responseMode", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = FetchSendResultStream.class, name = "stream"),
    @JsonSubTypes.Type(value = FetchSendResultText.class, name = "text"),
    @JsonSubTypes.Type(value = FetchSendResultFile.class, name = "file")
})
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class FetchSendResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the responseMode discriminator
     */
    public abstract FetchResponseMode getResponseMode();
}
