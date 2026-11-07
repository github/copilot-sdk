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
 * A Team marketplace fallback. Explicit marketplace configuration takes precedence.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TeamPluginMarketplace(
    /** Validated GitHub repository source for the marketplace. */
    @JsonProperty("source") TeamPluginMarketplaceSource source
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TeamPluginMarketplaceSource(
        /** Marketplace source discriminator. Currently only github is supported. */
        @JsonProperty("source") String source,
        /** GitHub repository in owner/name form. */
        @JsonProperty("repo") String repo
    ) {
    }
}
