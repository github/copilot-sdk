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
 * The runtime-selected image model and its account availability. Availability is independent of per-session image-generation enablement and does not override session policy or permissions.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageToolModel(
    /** Image model identifier selected by the runtime. */
    @JsonProperty("id") String id,
    /** Human-readable model name, supplied by the runtime when unavailable from the catalog. */
    @JsonProperty("name") String name,
    /** Image operations supported by the model's available versioned endpoints. */
    @JsonProperty("supportedOperations") List<ImageToolModelOperation> supportedOperations,
    /** Whether this is the model the runtime currently uses for its native image tool. */
    @JsonProperty("isDefault") Boolean isDefault,
    /** Whether the resolved account can use this image model. This is not the session's enabled setting. */
    @JsonProperty("availability") ImageToolModelAvailability availability,
    /** Catalog-reported token prices. Absent means prices were not reported. */
    @JsonProperty("pricing") ImageToolModelPricing pricing
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ImageToolModel {}

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
        private String id;
        private String name;
        private List<ImageToolModelOperation> supportedOperations;
        private Boolean isDefault;
        private ImageToolModelAvailability availability;
        private ImageToolModelPricing pricing;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code supportedOperations} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder supportedOperations(List<ImageToolModelOperation> value) {
            this.supportedOperations = value;
            return this;
        }

        /**
         * Sets the {@code isDefault} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isDefault(Boolean value) {
            this.isDefault = value;
            return this;
        }

        /**
         * Sets the {@code availability} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder availability(ImageToolModelAvailability value) {
            this.availability = value;
            return this;
        }

        /**
         * Sets the {@code pricing} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pricing(ImageToolModelPricing value) {
            this.pricing = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ImageToolModel build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (supportedOperations == null) throw new IllegalStateException("Missing required property: supportedOperations");
            if (isDefault == null) throw new IllegalStateException("Missing required property: isDefault");
            if (availability == null) throw new IllegalStateException("Missing required property: availability");
            return new ImageToolModel(id, name, supportedOperations, isDefault, availability, pricing);
        }
    }
}
