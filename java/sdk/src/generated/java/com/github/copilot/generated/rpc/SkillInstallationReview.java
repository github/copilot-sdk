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
 * Safe verified Skill review fields. No raw credential, candidate handle or plan handle.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "action", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SkillInstallationReviewInstall.class, name = "install"),
    @JsonSubTypes.Type(value = SkillInstallationReviewUninstall.class, name = "uninstall")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public abstract class SkillInstallationReview {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the action discriminator
     */
    public abstract String getAction();
}
