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
 * A normalized model-provider instance discovered by the runtime.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelProviderInstance(
    /** Self-contained reference for subsequent provider operations. */
    @JsonProperty("reference") ModelProviderInstanceReference reference,
    /** Human-readable instance name. */
    @JsonProperty("displayName") String displayName,
    /** Attribution for the adapter that produced this instance. */
    @JsonProperty("provenance") ModelProviderProvenance provenance,
    /** Inference API endpoint when the provider exposes one separately from its management endpoint. */
    @JsonProperty("inferenceEndpoint") String inferenceEndpoint,
    /** Provider family to use for inference against this instance. */
    @JsonProperty("inferenceType") ProviderEndpointType inferenceType,
    /** Wire API to use for inference against this instance, when required by the provider family. */
    @JsonProperty("inferenceWireApi") ProviderEndpointWireApi inferenceWireApi,
    /** Transport to use for inference against this instance. */
    @JsonProperty("inferenceTransport") ProviderEndpointTransport inferenceTransport
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelProviderInstance {}

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
        private ModelProviderInstanceReference reference;
        private String displayName;
        private ModelProviderProvenance provenance;
        private String inferenceEndpoint;
        private ProviderEndpointType inferenceType;
        private ProviderEndpointWireApi inferenceWireApi;
        private ProviderEndpointTransport inferenceTransport;

        /**
         * Sets the {@code reference} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reference(ModelProviderInstanceReference value) {
            this.reference = value;
            return this;
        }

        /**
         * Sets the {@code displayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder displayName(String value) {
            this.displayName = value;
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
         * Sets the {@code inferenceEndpoint} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder inferenceEndpoint(String value) {
            this.inferenceEndpoint = value;
            return this;
        }

        /**
         * Sets the {@code inferenceType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder inferenceType(ProviderEndpointType value) {
            this.inferenceType = value;
            return this;
        }

        /**
         * Sets the {@code inferenceWireApi} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder inferenceWireApi(ProviderEndpointWireApi value) {
            this.inferenceWireApi = value;
            return this;
        }

        /**
         * Sets the {@code inferenceTransport} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder inferenceTransport(ProviderEndpointTransport value) {
            this.inferenceTransport = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelProviderInstance build() {
            if (reference == null) throw new IllegalStateException("Missing required property: reference");
            if (displayName == null) throw new IllegalStateException("Missing required property: displayName");
            if (provenance == null) throw new IllegalStateException("Missing required property: provenance");
            return new ModelProviderInstance(reference, displayName, provenance, inferenceEndpoint, inferenceType, inferenceWireApi, inferenceTransport);
        }
    }
}
