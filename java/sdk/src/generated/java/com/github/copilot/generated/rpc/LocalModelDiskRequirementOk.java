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
 * Known disk space meets the installation requirement.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class LocalModelDiskRequirementOk extends LocalModelDiskRequirement {

    @JsonProperty("kind")
    private final String kind = "ok";

    @Override
    public String getKind() { return kind; }

    /** Available bytes on the destination filesystem. */
    @JsonProperty("freeBytes")
    private Double freeBytes;

    public Double getFreeBytes() { return freeBytes; }
    public void setFreeBytes(Double freeBytes) { this.freeBytes = freeBytes; }


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
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public LocalModelDiskRequirementOk build() {
            if (freeBytes == null) throw new IllegalStateException("Missing required property: freeBytes");
            LocalModelDiskRequirementOk instance = new LocalModelDiskRequirementOk();
            instance.setFreeBytes(freeBytes);
            return instance;
        }
    }
}
