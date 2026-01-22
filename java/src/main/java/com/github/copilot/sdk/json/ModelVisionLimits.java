/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Model vision-specific limits.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelVisionLimits {

    @JsonProperty("supported_media_types")
    private List<String> supportedMediaTypes;

    @JsonProperty("max_prompt_images")
    private int maxPromptImages;

    @JsonProperty("max_prompt_image_size")
    private int maxPromptImageSize;

    public List<String> getSupportedMediaTypes() {
        return supportedMediaTypes;
    }

    public ModelVisionLimits setSupportedMediaTypes(List<String> supportedMediaTypes) {
        this.supportedMediaTypes = supportedMediaTypes;
        return this;
    }

    public int getMaxPromptImages() {
        return maxPromptImages;
    }

    public ModelVisionLimits setMaxPromptImages(int maxPromptImages) {
        this.maxPromptImages = maxPromptImages;
        return this;
    }

    public int getMaxPromptImageSize() {
        return maxPromptImageSize;
    }

    public ModelVisionLimits setMaxPromptImageSize(int maxPromptImageSize) {
        this.maxPromptImageSize = maxPromptImageSize;
        return this;
    }
}
