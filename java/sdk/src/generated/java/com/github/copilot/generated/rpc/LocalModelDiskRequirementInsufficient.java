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
 * Known disk space does not meet the installation requirement.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class LocalModelDiskRequirementInsufficient extends LocalModelDiskRequirement {

    @JsonProperty("kind")
    private final String kind = "insufficient";

    @Override
    public String getKind() { return kind; }

    /** Available bytes on the destination filesystem. */
    @JsonProperty("freeBytes")
    private Double freeBytes;

    /** Required bytes, including the safety allowance. */
    @JsonProperty("requiredBytes")
    private Double requiredBytes;

    /** Destination used for the disk evaluation. */
    @JsonProperty("directory")
    private String directory;

    public Double getFreeBytes() { return freeBytes; }
    public void setFreeBytes(Double freeBytes) { this.freeBytes = freeBytes; }

    public Double getRequiredBytes() { return requiredBytes; }
    public void setRequiredBytes(Double requiredBytes) { this.requiredBytes = requiredBytes; }

    public String getDirectory() { return directory; }
    public void setDirectory(String directory) { this.directory = directory; }


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
        private Double freeBytes;
        private Double requiredBytes;
        private String directory;

        /**
         * Sets the {@code freeBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder freeBytes(Double value) {
            this.freeBytes = value;
            return this;
        }

        /**
         * Sets the {@code requiredBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requiredBytes(Double value) {
            this.requiredBytes = value;
            return this;
        }

        /**
         * Sets the {@code directory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder directory(String value) {
            this.directory = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelDiskRequirementInsufficient build() {
            if (freeBytes == null) throw new IllegalStateException("Missing required property: freeBytes");
            if (requiredBytes == null) throw new IllegalStateException("Missing required property: requiredBytes");
            if (directory == null) throw new IllegalStateException("Missing required property: directory");
            LocalModelDiskRequirementInsufficient instance = new LocalModelDiskRequirementInsufficient();
            instance.setFreeBytes(freeBytes);
            instance.setRequiredBytes(requiredBytes);
            instance.setDirectory(directory);
            return instance;
        }
    }
}
