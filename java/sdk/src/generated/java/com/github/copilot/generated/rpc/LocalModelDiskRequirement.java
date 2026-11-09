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
 * Local disk requirement evaluation.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = LocalModelDiskRequirementUnknown.class, name = "unknown"),
    @JsonSubTypes.Type(value = LocalModelDiskRequirementOk.class, name = "ok"),
    @JsonSubTypes.Type(value = LocalModelDiskRequirementInsufficient.class, name = "insufficient")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class LocalModelDiskRequirement {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
