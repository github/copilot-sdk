/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Vision-specific limits
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelCapabilitiesLimitsVision(
    /** MIME types the model accepts */
    @JsonProperty("supported_media_types") List<String> supportedMediaTypes,
    /** Maximum number of images per prompt */
    @JsonProperty("max_prompt_images") Long maxPromptImages,
    /** Maximum image size in bytes */
    @JsonProperty("max_prompt_image_size") Long maxPromptImageSize
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelCapabilitiesLimitsVision {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private List<String> supportedMediaTypes;
        private Long maxPromptImages;
        private Long maxPromptImageSize;

        /**
         * Sets the {@code supported_media_types} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder supportedMediaTypes(List<String> value) {
            this.supportedMediaTypes = value;
            return this;
        }

        /**
         * Sets the {@code max_prompt_images} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxPromptImages(Long value) {
            this.maxPromptImages = value;
            return this;
        }

        /**
         * Sets the {@code max_prompt_image_size} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxPromptImageSize(Long value) {
            this.maxPromptImageSize = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelCapabilitiesLimitsVision build() {
            if (supportedMediaTypes == null) throw new IllegalStateException("Missing required property: supported_media_types");
            if (maxPromptImages == null) throw new IllegalStateException("Missing required property: max_prompt_images");
            if (maxPromptImageSize == null) throw new IllegalStateException("Missing required property: max_prompt_image_size");
            return new ModelCapabilitiesLimitsVision(supportedMediaTypes, maxPromptImages, maxPromptImageSize);
        }
    }
}
