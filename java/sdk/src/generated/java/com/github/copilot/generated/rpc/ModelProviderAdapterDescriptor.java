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
 * A normalized model-provider adapter in the session's effective catalog.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelProviderAdapterDescriptor(
    /** Stable opaque identity for routing to this adapter. Unique in the effective catalog, independent of live registration generations. */
    @JsonProperty("adapterId") String adapterId,
    /** Descriptive provider family, such as `ollama`. Different adapters may have the same family; use adapterId for routing. */
    @JsonProperty("providerKind") String providerKind,
    /** Human-readable provider name. */
    @JsonProperty("displayName") String displayName,
    /** Attribution for the adapter itself. */
    @JsonProperty("provenance") ModelProviderAttribution provenance,
    /** Adapter-declared policy for passive and automatic discovery. */
    @JsonProperty("automaticDiscovery") ModelProviderAutomaticDiscoveryPolicy automaticDiscovery,
    /** Operations supported by this provider adapter. */
    @JsonProperty("operations") List<ModelProviderAdapterOperationDescriptor> operations
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelProviderAdapterDescriptor {}

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
        private String adapterId;
        private String providerKind;
        private String displayName;
        private ModelProviderAttribution provenance;
        private ModelProviderAutomaticDiscoveryPolicy automaticDiscovery;
        private List<ModelProviderAdapterOperationDescriptor> operations;

        /**
         * Sets the {@code adapterId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder adapterId(String value) {
            this.adapterId = value;
            return this;
        }

        /**
         * Sets the {@code providerKind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder providerKind(String value) {
            this.providerKind = value;
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
        public Builder provenance(ModelProviderAttribution value) {
            this.provenance = value;
            return this;
        }

        /**
         * Sets the {@code automaticDiscovery} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder automaticDiscovery(ModelProviderAutomaticDiscoveryPolicy value) {
            this.automaticDiscovery = value;
            return this;
        }

        /**
         * Sets the {@code operations} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder operations(List<ModelProviderAdapterOperationDescriptor> value) {
            this.operations = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelProviderAdapterDescriptor build() {
            if (adapterId == null) throw new IllegalStateException("Missing required property: adapterId");
            if (providerKind == null) throw new IllegalStateException("Missing required property: providerKind");
            if (displayName == null) throw new IllegalStateException("Missing required property: displayName");
            if (provenance == null) throw new IllegalStateException("Missing required property: provenance");
            if (automaticDiscovery == null) throw new IllegalStateException("Missing required property: automaticDiscovery");
            if (operations == null) throw new IllegalStateException("Missing required property: operations");
            return new ModelProviderAdapterDescriptor(adapterId, providerKind, displayName, provenance, automaticDiscovery, operations);
        }
    }
}
