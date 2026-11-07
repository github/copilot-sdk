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
import javax.annotation.processing.Generated;

/**
 * Eligible Azure ML model version and its provider metadata.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AzureMlLatestResult(
    /** Registry that owns this model version. */
    @JsonProperty("registry") AzureMlRegistry registry,
    /** Stable model-family name. */
    @JsonProperty("name") String name,
    /** Exact published model version. */
    @JsonProperty("version") String version,
    /** Validated Windows ML requirements. */
    @JsonProperty("profile") AzureMlProfile profile,
    /** Provider metadata retained for registration. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("metadata") Object metadata
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AzureMlLatestResult {}

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
        private AzureMlRegistry registry;
        private String name;
        private String version;
        private AzureMlProfile profile;
        private Object metadata;
        private boolean $presence4;

        /**
         * Sets the {@code registry} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder registry(AzureMlRegistry value) {
            this.registry = value;
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
         * Sets the {@code version} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder version(String value) {
            this.version = value;
            return this;
        }

        /**
         * Sets the {@code profile} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder profile(AzureMlProfile value) {
            this.profile = value;
            return this;
        }

        /**
         * Sets the {@code metadata} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder metadata(Object value) {
            this.metadata = value;
            this.$presence4 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AzureMlLatestResult build() {
            if (registry == null) throw new IllegalStateException("Missing required property: registry");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (version == null) throw new IllegalStateException("Missing required property: version");
            if (profile == null) throw new IllegalStateException("Missing required property: profile");
            if (!$presence4) throw new IllegalStateException("Missing required property: metadata");
            return new AzureMlLatestResult(registry, name, version, profile, metadata);
        }
    }
}
