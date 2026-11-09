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
 * A valid activation references the local model files.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class LocalModelStatusDownloaded extends LocalModelStatus {

    @JsonProperty("kind")
    private final String kind = "downloaded";

    @Override
    public String getKind() { return kind; }

    /** Local file size, when known. */
    @JsonProperty("sizeBytes")
    private Double sizeBytes;

    /** Whether the catalog offers another version. */
    @JsonProperty("updateAvailable")
    private Boolean updateAvailable;

    public Double getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Double sizeBytes) { this.sizeBytes = sizeBytes; }

    public Boolean getUpdateAvailable() { return updateAvailable; }
    public void setUpdateAvailable(Boolean updateAvailable) { this.updateAvailable = updateAvailable; }


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
        private Double sizeBytes;
        private Boolean updateAvailable;

        /**
         * Sets the {@code sizeBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sizeBytes(Double value) {
            this.sizeBytes = value;
            return this;
        }

        /**
         * Sets the {@code updateAvailable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder updateAvailable(Boolean value) {
            this.updateAvailable = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelStatusDownloaded build() {
            LocalModelStatusDownloaded instance = new LocalModelStatusDownloaded();
            instance.setSizeBytes(sizeBytes);
            instance.setUpdateAvailable(updateAvailable);
            return instance;
        }
    }
}
