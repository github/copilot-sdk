/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Durable verified Skill ownership summary.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillInstallationSummary(
    /** Exact durable installation receipt identity. */
    @JsonProperty("installationId") String installationId,
    /** Operation that installed this Skill. */
    @JsonProperty("operationId") String operationId,
    /** Skill invocation name. */
    @JsonProperty("name") String name,
    /** User-facing installation location without an absolute host path. */
    @JsonProperty("target") SkillInstallationLocation target,
    /** Persisted enablement requested for this installation. */
    @JsonProperty("configuredEnabled") Boolean configuredEnabled,
    /** Bound-session load observation. */
    @JsonProperty("sessionState") SkillInstallationSessionState sessionState,
    /** Exact retained verified source identity. */
    @JsonProperty("source") SkillInstallationSource source,
    /** Catalogue identity retained at install time. */
    @JsonProperty("catalogue") InstallationCatalogueIdentity catalogue,
    /** ISO 8601 wall-clock installation time. */
    @JsonProperty("installedAt") String installedAt,
    /** Ownership state observed from files and receipts. */
    @JsonProperty("ownershipState") SkillInstallationOwnershipState ownershipState
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SkillInstallationSummary {}

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
        private String operationId;
        private String name;
        private SkillInstallationLocation target;
        private Boolean configuredEnabled;
        private SkillInstallationSessionState sessionState;
        private SkillInstallationSource source;
        private InstallationCatalogueIdentity catalogue;
        private String installedAt;
        private SkillInstallationOwnershipState ownershipState;

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
         * Sets the {@code operationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder operationId(String value) {
            this.operationId = value;
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
         * Sets the {@code target} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder target(SkillInstallationLocation value) {
            this.target = value;
            return this;
        }

        /**
         * Sets the {@code configuredEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder configuredEnabled(Boolean value) {
            this.configuredEnabled = value;
            return this;
        }

        /**
         * Sets the {@code sessionState} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionState(SkillInstallationSessionState value) {
            this.sessionState = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(SkillInstallationSource value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code catalogue} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder catalogue(InstallationCatalogueIdentity value) {
            this.catalogue = value;
            return this;
        }

        /**
         * Sets the {@code installedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installedAt(String value) {
            this.installedAt = value;
            return this;
        }

        /**
         * Sets the {@code ownershipState} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder ownershipState(SkillInstallationOwnershipState value) {
            this.ownershipState = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SkillInstallationSummary build() {
            if (installationId == null) throw new IllegalStateException("Missing required property: installationId");
            if (operationId == null) throw new IllegalStateException("Missing required property: operationId");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (target == null) throw new IllegalStateException("Missing required property: target");
            if (configuredEnabled == null) throw new IllegalStateException("Missing required property: configuredEnabled");
            if (sessionState == null) throw new IllegalStateException("Missing required property: sessionState");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (catalogue == null) throw new IllegalStateException("Missing required property: catalogue");
            if (installedAt == null) throw new IllegalStateException("Missing required property: installedAt");
            if (ownershipState == null) throw new IllegalStateException("Missing required property: ownershipState");
            return new SkillInstallationSummary(installationId, operationId, name, target, configuredEnabled, sessionState, source, catalogue, installedAt, ownershipState);
        }
    }
}
