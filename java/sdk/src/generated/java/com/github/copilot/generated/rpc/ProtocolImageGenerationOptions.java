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
 * Opt into native CAPI image generation.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProtocolImageGenerationOptions(
    /** Disabled by default for SDK sessions. CAPI model access and permissions still apply. */
    @JsonProperty("enabled") Boolean enabled,
    /** Experimental. Request a visible "Made with AI" label on generated and edited images.
This option may change or be removed in a future release.
Overrides the user's imageGenerationWatermark preference for this session;
otherwise that preference applies, defaulting to false. Re-supply after a
cold resume. Does not enable image generation or enforce enterprise policy. */
    @JsonProperty("watermark") Boolean watermark
) {

    /**
     * Creates image generation options without an explicit watermark preference.
     *
     * @param enabled Disabled by default for SDK sessions. CAPI model access and permissions still apply.
     */
    public ProtocolImageGenerationOptions(
        Boolean enabled
    ) {
        this(enabled, null);
    }
}
