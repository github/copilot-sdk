/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * A possible secret-bearing environment variable and optional locally suggested HTTPS injection hosts. This is a draft for user review, not an active grant.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxCredentialSuggestion(
    /** Environment variable name. The secret value is never returned. */
    @JsonProperty("name") String name,
    /** HTTPS hostnames suggested by a local known-provider mapping. An empty list requires the user to supply injection hosts before adding a masking entry. These suggestions do not allow network access. */
    @JsonProperty("suggestedInjectHosts") List<String> suggestedInjectHosts
) {
}
