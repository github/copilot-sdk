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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Provider-reported model artifact metadata.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelArtifactDetails(
    /** Artifact format, such as `gguf`. */
    @JsonProperty("format") String format,
    /** Primary model family. */
    @JsonProperty("family") String family,
    /** Provider-reported model families. */
    @JsonProperty("families") List<String> families,
    /** Provider-reported parameter count label. */
    @JsonProperty("parameterSize") String parameterSize,
    /** Provider-reported quantization label. */
    @JsonProperty("quantization") String quantization,
    /** Provider-reported model architecture. */
    @JsonProperty("architecture") String architecture,
    /** Provider-reported tokenizer. */
    @JsonProperty("tokenizer") String tokenizer
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelArtifactDetails {}

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
        private String format;
        private String family;
        private List<String> families;
        private String parameterSize;
        private String quantization;
        private String architecture;
        private String tokenizer;

        /**
         * Sets the {@code format} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder format(String value) {
            this.format = value;
            return this;
        }

        /**
         * Sets the {@code family} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder family(String value) {
            this.family = value;
            return this;
        }

        /**
         * Sets the {@code families} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder families(List<String> value) {
            this.families = value;
            return this;
        }

        /**
         * Sets the {@code parameterSize} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder parameterSize(String value) {
            this.parameterSize = value;
            return this;
        }

        /**
         * Sets the {@code quantization} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder quantization(String value) {
            this.quantization = value;
            return this;
        }

        /**
         * Sets the {@code architecture} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder architecture(String value) {
            this.architecture = value;
            return this;
        }

        /**
         * Sets the {@code tokenizer} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder tokenizer(String value) {
            this.tokenizer = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelArtifactDetails build() {
            return new ModelArtifactDetails(format, family, families, parameterSize, quantization, architecture, tokenizer);
        }
    }
}
