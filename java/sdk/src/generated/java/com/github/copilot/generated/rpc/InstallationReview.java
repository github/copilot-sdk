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
 * Only resource kinds with an implemented installation engine have a review variant.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "resource", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = InstallationReviewMcp.class, name = "mcp"),
    @JsonSubTypes.Type(value = InstallationReviewSkill.class, name = "skill")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class InstallationReview {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the resource discriminator
     */
    public abstract String getResource();
}
