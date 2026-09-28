/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.processing.Generated;

/**
 * Safe MCP review fields. No raw card, retrieval URL, plan handle or secret value.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "action", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = McpInstallationReviewInstall.class, name = "install"),
    @JsonSubTypes.Type(value = McpInstallationReviewUninstall.class, name = "uninstall")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public sealed interface McpInstallationReview permits McpInstallationReviewInstall, McpInstallationReviewUninstall {
    /**
     * Returns the discriminator value for this variant.
     *
     * @return the action discriminator
     */
    @JsonProperty("action")
    String action();
}
