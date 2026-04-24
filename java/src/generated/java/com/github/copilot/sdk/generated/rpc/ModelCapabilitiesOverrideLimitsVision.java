/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelCapabilitiesOverrideLimitsVision(
    /** MIME types the model accepts */
    @JsonProperty("supported_media_types") List<String> supportedMediaTypes,
    /** Maximum number of images per prompt */
    @JsonProperty("max_prompt_images") Long maxPromptImages,
    /** Maximum image size in bytes */
    @JsonProperty("max_prompt_image_size") Long maxPromptImageSize
) {
}
