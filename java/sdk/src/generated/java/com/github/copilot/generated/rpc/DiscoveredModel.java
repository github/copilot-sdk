/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.time.OffsetDateTime;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * A model offered for agent conversations. Missing capability metadata does not disqualify a candidate. Models known to be incompatible, such as embedding-only models, are excluded by the adapter.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscoveredModel(
    /** Provider-native model identifier. */
    @JsonProperty("id") String id,
    /** Provider-reported display name. */
    @JsonProperty("name") String name,
    /** Attribution for the adapter that produced this model row. */
    @JsonProperty("provenance") ModelProviderProvenance provenance,
    /** Provider-reported artifact digest. */
    @JsonProperty("digest") String digest,
    /** Provider-reported last-modified timestamp. */
    @JsonProperty("modifiedAt") OffsetDateTime modifiedAt,
    /** Provider-reported artifact size in bytes. */
    @JsonProperty("sizeBytes") Long sizeBytes,
    /** Provider-reported model artifact details. */
    @JsonProperty("details") ModelArtifactDetails details,
    /** Provider-reported model capabilities. Omitted capability fields are unknown; explicit false values are preserved. */
    @JsonProperty("capabilities") ModelCapabilities capabilities,
    /** Non-fatal warnings encountered while enriching this model. */
    @JsonProperty("warnings") List<ModelProviderWarning> warnings
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DiscoveredModel {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String id;
        private String name;
        private ModelProviderProvenance provenance;
        private String digest;
        private OffsetDateTime modifiedAt;
        private Long sizeBytes;
        private ModelArtifactDetails details;
        private ModelCapabilities capabilities;
        private List<ModelProviderWarning> warnings;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code provenance} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder provenance(ModelProviderProvenance value) {
            this.provenance = value;
            return this;
        }

        /**
         * Sets the {@code digest} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder digest(String value) {
            this.digest = value;
            return this;
        }

        /**
         * Sets the {@code modifiedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder modifiedAt(OffsetDateTime value) {
            this.modifiedAt = value;
            return this;
        }

        /**
         * Sets the {@code sizeBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sizeBytes(Long value) {
            this.sizeBytes = value;
            return this;
        }

        /**
         * Sets the {@code details} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder details(ModelArtifactDetails value) {
            this.details = value;
            return this;
        }

        /**
         * Sets the {@code capabilities} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder capabilities(ModelCapabilities value) {
            this.capabilities = value;
            return this;
        }

        /**
         * Sets the {@code warnings} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder warnings(List<ModelProviderWarning> value) {
            this.warnings = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public DiscoveredModel build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (provenance == null) throw new IllegalStateException("Missing required property: provenance");
            if (details == null) throw new IllegalStateException("Missing required property: details");
            if (capabilities == null) throw new IllegalStateException("Missing required property: capabilities");
            if (warnings == null) throw new IllegalStateException("Missing required property: warnings");
            return new DiscoveredModel(id, name, provenance, digest, modifiedAt, sizeBytes, details, capabilities, warnings);
        }
    }
}
