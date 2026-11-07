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
 * Serializable reference to a discovered provider instance.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelProviderInstanceReference(
    /** Stable opaque identity of the adapter that owns this reference. Must be present in the target session's effective catalog. */
    @JsonProperty("adapterId") String adapterId,
    /** Descriptive provider family. Must match the selected adapter; not a routing key. */
    @JsonProperty("providerKind") String providerKind,
    /** Stable instance identifier derived by the provider adapter, such as `ollama:{normalizedEndpoint}`. */
    @JsonProperty("id") String id,
    /** Absolute provider management URI. The adapter validates normalization, supported schemes, and permission to access it against its bound configuration; a reference does not grant authority. */
    @JsonProperty("managementEndpoint") String managementEndpoint
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelProviderInstanceReference {}

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
        private String id;
        private String managementEndpoint;

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
         * Sets the {@code managementEndpoint} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder managementEndpoint(String value) {
            this.managementEndpoint = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelProviderInstanceReference build() {
            if (adapterId == null) throw new IllegalStateException("Missing required property: adapterId");
            if (providerKind == null) throw new IllegalStateException("Missing required property: providerKind");
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (managementEndpoint == null) throw new IllegalStateException("Missing required property: managementEndpoint");
            return new ModelProviderInstanceReference(adapterId, providerKind, id, managementEndpoint);
        }
    }
}
