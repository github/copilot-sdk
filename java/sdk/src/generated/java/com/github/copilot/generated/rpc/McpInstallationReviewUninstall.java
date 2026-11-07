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
import javax.annotation.processing.Generated;

/**
 * Variant {@code uninstall} of {@link McpInstallationReview}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public record McpInstallationReviewUninstall(
    /** Receipt-owned installation being removed. */
    @JsonProperty("installationId") String installationId,
    /** Identity from the installed receipt. */
    @JsonProperty("identity") McpPlanResourceIdentity identity,
    /** Source identity and content commitment retained by the installed receipt. */
    @JsonProperty("provenance") McpPlanProvenance provenance,
    /** Exact destination, checked for intervening changes before mutation. */
    @JsonProperty("target") McpPlanTarget target,
    /** Current removal policy, independent of permission to activate the server. */
    @JsonProperty("policy") McpPlanPolicyResult policy,
    /** Whether uninstall restores a protected pre-install configuration. */
    @JsonProperty("restoresPreviousConfiguration") Boolean restoresPreviousConfiguration,
    /** Exact planner-owned secret slots to remove, excluding shared OAuth grants. */
    @JsonProperty("ownedSecretCount") Long ownedSecretCount,
    /** Shared profile authentication is deliberately retained, not pending cleanup. */
    @JsonProperty("preservesSharedAuthentication") Boolean preservesSharedAuthentication,
    /** Exact reviewed installation action. */
    @JsonProperty("action") String action
) implements McpInstallationReview {
    /**
     * Creates this variant positionally and normalizes its discriminator.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpInstallationReviewUninstall {
        action = "uninstall";
    }

    /**
     * Creates this variant without explicitly supplying its discriminator.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpInstallationReviewUninstall(
        String installationId,
        McpPlanResourceIdentity identity,
        McpPlanProvenance provenance,
        McpPlanTarget target,
        McpPlanPolicyResult policy,
        Boolean restoresPreviousConfiguration,
        Long ownedSecretCount,
        Boolean preservesSharedAuthentication
    ) {
        this(installationId, identity, provenance, target, policy, restoresPreviousConfiguration, ownedSecretCount, preservesSharedAuthentication, "uninstall");
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
        private String installationId;
        private McpPlanResourceIdentity identity;
        private McpPlanProvenance provenance;
        private McpPlanTarget target;
        private McpPlanPolicyResult policy;
        private Boolean restoresPreviousConfiguration;
        private Long ownedSecretCount;
        private Boolean preservesSharedAuthentication;

        /**
         * Sets the {@code installationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installationId(String value) {
            this.installationId = value;
            return this;
        }

        /**
         * Sets the {@code identity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder identity(McpPlanResourceIdentity value) {
            this.identity = value;
            return this;
        }

        /**
         * Sets the {@code provenance} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder provenance(McpPlanProvenance value) {
            this.provenance = value;
            return this;
        }

        /**
         * Sets the {@code target} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder target(McpPlanTarget value) {
            this.target = value;
            return this;
        }

        /**
         * Sets the {@code policy} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policy(McpPlanPolicyResult value) {
            this.policy = value;
            return this;
        }

        /**
         * Sets the {@code restoresPreviousConfiguration} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder restoresPreviousConfiguration(Boolean value) {
            this.restoresPreviousConfiguration = value;
            return this;
        }

        /**
         * Sets the {@code ownedSecretCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder ownedSecretCount(Long value) {
            this.ownedSecretCount = value;
            return this;
        }

        /**
         * Sets the {@code preservesSharedAuthentication} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder preservesSharedAuthentication(Boolean value) {
            this.preservesSharedAuthentication = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpInstallationReviewUninstall build() {
            if (installationId == null) throw new IllegalStateException("Missing required property: installationId");
            if (identity == null) throw new IllegalStateException("Missing required property: identity");
            if (provenance == null) throw new IllegalStateException("Missing required property: provenance");
            if (target == null) throw new IllegalStateException("Missing required property: target");
            if (policy == null) throw new IllegalStateException("Missing required property: policy");
            if (restoresPreviousConfiguration == null) throw new IllegalStateException("Missing required property: restoresPreviousConfiguration");
            if (ownedSecretCount == null) throw new IllegalStateException("Missing required property: ownedSecretCount");
            if (preservesSharedAuthentication == null) throw new IllegalStateException("Missing required property: preservesSharedAuthentication");
            return new McpInstallationReviewUninstall(installationId, identity, provenance, target, policy, restoresPreviousConfiguration, ownedSecretCount, preservesSharedAuthentication, "uninstall");
        }
    }
}
