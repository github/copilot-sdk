/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Variant {@code skill} of {@link InstallationReview}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class InstallationReviewSkill extends InstallationReview {

    @JsonProperty("resource")
    private final String resource = "skill";

    @Override
    public String getResource() { return resource; }

    /** The exact verified Skill action and its reviewed files. */
    @JsonProperty("review")
    private SkillInstallationReview review;

    public SkillInstallationReview getReview() { return review; }
    public void setReview(SkillInstallationReview review) { this.review = review; }
}
