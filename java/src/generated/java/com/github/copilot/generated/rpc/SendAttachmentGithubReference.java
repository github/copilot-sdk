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
 * GitHub issue, pull request, or discussion reference
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SendAttachmentGithubReference extends SendAttachment {

    @JsonProperty("type")
    private final String type = "github_reference";

    @Override
    public String getType() { return type; }

    /** Issue, pull request, or discussion number */
    @JsonProperty("number")
    private Long number;

    /** Title of the referenced item */
    @JsonProperty("title")
    private String title;

    /** Type of GitHub reference */
    @JsonProperty("referenceType")
    private SendAttachmentGithubReferenceType referenceType;

    /** Current state of the referenced item (e.g., open, closed, merged) */
    @JsonProperty("state")
    private String state;

    /** URL to the referenced item on GitHub */
    @JsonProperty("url")
    private String url;

    public Long getNumber() { return number; }
    public void setNumber(Long number) { this.number = number; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public SendAttachmentGithubReferenceType getReferenceType() { return referenceType; }
    public void setReferenceType(SendAttachmentGithubReferenceType referenceType) { this.referenceType = referenceType; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
