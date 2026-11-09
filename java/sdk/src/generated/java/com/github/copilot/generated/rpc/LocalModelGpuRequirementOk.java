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
 * Detected graphics memory meets the published requirement.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class LocalModelGpuRequirementOk extends LocalModelGpuRequirement {

    @JsonProperty("kind")
    private final String kind = "ok";

    @Override
    public String getKind() { return kind; }

    /** Available graphics memory. */
    @JsonProperty("availableBytes")
    private Double availableBytes;

    /** Published recommended graphics memory. */
    @JsonProperty("requiredBytes")
    private Double requiredBytes;

    /** Graphics adapter description. */
    @JsonProperty("description")
    private String description;

    public Double getAvailableBytes() { return availableBytes; }
    public void setAvailableBytes(Double availableBytes) { this.availableBytes = availableBytes; }

    public Double getRequiredBytes() { return requiredBytes; }
    public void setRequiredBytes(Double requiredBytes) { this.requiredBytes = requiredBytes; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }


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
        private Double availableBytes;
        private Double requiredBytes;
        private String description;

        /**
         * Sets the {@code availableBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder availableBytes(Double value) {
            this.availableBytes = value;
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
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelGpuRequirementOk build() {
            if (availableBytes == null) throw new IllegalStateException("Missing required property: availableBytes");
            if (requiredBytes == null) throw new IllegalStateException("Missing required property: requiredBytes");
            if (description == null) throw new IllegalStateException("Missing required property: description");
            LocalModelGpuRequirementOk instance = new LocalModelGpuRequirementOk();
            instance.setAvailableBytes(availableBytes);
            instance.setRequiredBytes(requiredBytes);
            instance.setDescription(description);
            return instance;
        }
    }
}
