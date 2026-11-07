/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * An eligible remote-endpoint transport choice. The endpoint is required and package identity cannot be represented.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record McpPlanTransportChoiceRemote(
    /** Stable identifier for this choice within the plan, used to select it when the plan is applied. */
    @JsonProperty("choiceId") String choiceId,
    /** Endpoint transport this remote choice would use. */
    @JsonProperty("transport") McpPlanRemoteTransport transport,
    /** Discriminator: this choice connects to a remote endpoint */
    @JsonProperty("installMethod") String installMethod,
    /** Endpoint URL. Inert untrusted data. */
    @JsonProperty("endpoint") String endpoint,
    /** Typed values this choice requires, excluding secrets. */
    @JsonProperty("requiredValues") List<Object> requiredValues,
    /** Secrets this choice requires, referenced by placeholder only. */
    @JsonProperty("secretPlaceholders") List<McpPlanSecretPlaceholder> secretPlaceholders
) implements McpPlanTransportChoice {
    /**
     * Creates this variant positionally and normalizes its discriminator.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpPlanTransportChoiceRemote {
        installMethod = "remote";
    }

    /**
     * Creates this variant without explicitly supplying its discriminator.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpPlanTransportChoiceRemote(
        String choiceId,
        McpPlanRemoteTransport transport,
        String endpoint,
        List<Object> requiredValues,
        List<McpPlanSecretPlaceholder> secretPlaceholders
    ) {
        this(choiceId, transport, "remote", endpoint, requiredValues, secretPlaceholders);
    }

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
        private String choiceId;
        private McpPlanRemoteTransport transport;
        private String endpoint;
        private List<Object> requiredValues;
        private List<McpPlanSecretPlaceholder> secretPlaceholders;

        /**
         * Sets the {@code choiceId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder choiceId(String value) {
            this.choiceId = value;
            return this;
        }

        /**
         * Sets the {@code transport} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder transport(McpPlanRemoteTransport value) {
            this.transport = value;
            return this;
        }

        /**
         * Sets the {@code endpoint} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder endpoint(String value) {
            this.endpoint = value;
            return this;
        }

        /**
         * Sets the {@code requiredValues} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requiredValues(List<Object> value) {
            this.requiredValues = value;
            return this;
        }

        /**
         * Sets the {@code secretPlaceholders} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder secretPlaceholders(List<McpPlanSecretPlaceholder> value) {
            this.secretPlaceholders = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpPlanTransportChoiceRemote build() {
            if (choiceId == null) throw new IllegalStateException("Missing required property: choiceId");
            if (transport == null) throw new IllegalStateException("Missing required property: transport");
            if (endpoint == null) throw new IllegalStateException("Missing required property: endpoint");
            if (requiredValues == null) throw new IllegalStateException("Missing required property: requiredValues");
            if (secretPlaceholders == null) throw new IllegalStateException("Missing required property: secretPlaceholders");
            return new McpPlanTransportChoiceRemote(choiceId, transport, "remote", endpoint, requiredValues, secretPlaceholders);
        }
    }
}
