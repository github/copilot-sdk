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
 * A transfer or inference engine installation is active.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class LocalModelStatusDownloading extends LocalModelStatus {

    @JsonProperty("kind")
    private final String kind = "downloading";

    @Override
    public String getKind() { return kind; }

    /** Bytes received so far. */
    @JsonProperty("receivedBytes")
    private Double receivedBytes;

    /** Total transfer bytes, when known. */
    @JsonProperty("totalBytes")
    private Double totalBytes;

    /** Inference engine installation, when applicable. */
    @JsonProperty("stage")
    private String stage;

    public Double getReceivedBytes() { return receivedBytes; }
    public void setReceivedBytes(Double receivedBytes) { this.receivedBytes = receivedBytes; }

    public Double getTotalBytes() { return totalBytes; }
    public void setTotalBytes(Double totalBytes) { this.totalBytes = totalBytes; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }


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
        private Double receivedBytes;
        private Double totalBytes;
        private String stage;

        /**
         * Sets the {@code receivedBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder receivedBytes(Double value) {
            this.receivedBytes = value;
            return this;
        }

        /**
         * Sets the {@code totalBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder totalBytes(Double value) {
            this.totalBytes = value;
            return this;
        }

        /**
         * Sets the {@code stage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder stage(String value) {
            this.stage = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelStatusDownloading build() {
            if (receivedBytes == null) throw new IllegalStateException("Missing required property: receivedBytes");
            LocalModelStatusDownloading instance = new LocalModelStatusDownloading();
            instance.setReceivedBytes(receivedBytes);
            instance.setTotalBytes(totalBytes);
            instance.setStage(stage);
            return instance;
        }
    }
}
