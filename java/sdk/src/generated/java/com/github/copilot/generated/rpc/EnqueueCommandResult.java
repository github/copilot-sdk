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
 * Indicates whether the command was accepted into the local execution queue.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "queued", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = EnqueueCommandResultTrue.class, name = "true"),
    @JsonSubTypes.Type(value = EnqueueCommandResultFalse.class, name = "false")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class EnqueueCommandResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the queued discriminator
     */
    public abstract String getQueued();
}
