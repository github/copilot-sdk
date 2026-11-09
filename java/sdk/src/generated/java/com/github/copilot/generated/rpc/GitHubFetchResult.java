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
 * The HTTP response or a classified request failure.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = GitHubFetchResultResponse.class, name = "response"),
    @JsonSubTypes.Type(value = GitHubFetchResultCircuitBreaker.class, name = "circuit-breaker"),
    @JsonSubTypes.Type(value = GitHubFetchResultProxyResponse.class, name = "proxy-response"),
    @JsonSubTypes.Type(value = GitHubFetchResultFailed.class, name = "failed")
})
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
abstract class GitHubFetchResult {

    /**
     * Returns the discriminator value for this variant.
     *
     * @return the kind discriminator
     */
    public abstract String getKind();
}
