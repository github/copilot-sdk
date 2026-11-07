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
 * An eligible local-package transport choice. Package identity is required and a remote endpoint cannot be represented.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record McpPlanTransportChoicePackage(
    /** Stable identifier for this choice within the plan, used to select it when the plan is applied. */
    @JsonProperty("choiceId") String choiceId,
    /** Local process transport this package choice would use. */
    @JsonProperty("transport") McpPlanPackageTransport transport,
    /** Discriminator: this choice runs a local package */
    @JsonProperty("installMethod") String installMethod,
    /** Packaging ecosystem, for example `oci` or `npm`. */
    @JsonProperty("packageType") String packageType,
    /** Package identifier. Inert untrusted data. */
    @JsonProperty("packageIdentifier") String packageIdentifier,
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
    public McpPlanTransportChoicePackage {
        installMethod = "package";
    }

    /**
     * Creates this variant without explicitly supplying its discriminator.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpPlanTransportChoicePackage(
        String choiceId,
        McpPlanPackageTransport transport,
        String packageType,
        String packageIdentifier,
        List<Object> requiredValues,
        List<McpPlanSecretPlaceholder> secretPlaceholders
    ) {
        this(choiceId, transport, "package", packageType, packageIdentifier, requiredValues, secretPlaceholders);
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
        private McpPlanPackageTransport transport;
        private String packageType;
        private String packageIdentifier;
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
        public Builder transport(McpPlanPackageTransport value) {
            this.transport = value;
            return this;
        }

        /**
         * Sets the {@code packageType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder packageType(String value) {
            this.packageType = value;
            return this;
        }

        /**
         * Sets the {@code packageIdentifier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder packageIdentifier(String value) {
            this.packageIdentifier = value;
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
        public McpPlanTransportChoicePackage build() {
            if (choiceId == null) throw new IllegalStateException("Missing required property: choiceId");
            if (transport == null) throw new IllegalStateException("Missing required property: transport");
            if (packageType == null) throw new IllegalStateException("Missing required property: packageType");
            if (packageIdentifier == null) throw new IllegalStateException("Missing required property: packageIdentifier");
            if (requiredValues == null) throw new IllegalStateException("Missing required property: requiredValues");
            if (secretPlaceholders == null) throw new IllegalStateException("Missing required property: secretPlaceholders");
            return new McpPlanTransportChoicePackage(choiceId, transport, "package", packageType, packageIdentifier, requiredValues, secretPlaceholders);
        }
    }
}
