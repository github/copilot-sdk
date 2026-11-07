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
 * Registered marketplace summary.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketplaceInfo(
    /** Marketplace name (matches the @marketplace suffix in plugin specs) */
    @JsonProperty("name") String name,
    /** Human-readable description of where the marketplace data is fetched from (e.g. "GitHub: owner/repo"). */
    @JsonProperty("source") String source,
    /** True when this is a default marketplace shipped with the runtime. Defaults are not removable. */
    @JsonProperty("isDefault") Boolean isDefault,
    /** Whether enterprise managed settings provide and control this marketplace entry. */
    @JsonProperty("managed") Boolean managed,
    /** Whether the managed marketplace currently resolved into the runtime marketplace registry. Set to false when the desired managed entry is retained for governance visibility after loading or reconciliation failed. */
    @JsonProperty("available") Boolean available,
    /** True when the signed-in account's GitHub Team provides this marketplace and no user, repository, or default entry of the same name exists. Team marketplaces are not persisted, so they cannot be removed. */
    @JsonProperty("team") Boolean team
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public MarketplaceInfo {}

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
        private String name;
        private String source;
        private Boolean isDefault;
        private Boolean managed;
        private Boolean available;
        private Boolean team;

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
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(String value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code isDefault} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isDefault(Boolean value) {
            this.isDefault = value;
            return this;
        }

        /**
         * Sets the {@code managed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder managed(Boolean value) {
            this.managed = value;
            return this;
        }

        /**
         * Sets the {@code available} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder available(Boolean value) {
            this.available = value;
            return this;
        }

        /**
         * Sets the {@code team} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder team(Boolean value) {
            this.team = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public MarketplaceInfo build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            return new MarketplaceInfo(name, source, isDefault, managed, available, team);
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param name Marketplace name (matches the @marketplace suffix in plugin specs)
     * @param source Human-readable description of where the marketplace data is fetched from (e.g. "GitHub: owner/repo").
     * @param isDefault True when this is a default marketplace shipped with the runtime. Defaults are not removable.
     * @param managed Whether enterprise managed settings provide and control this marketplace entry.
     * @param available Whether the managed marketplace currently resolved into the runtime marketplace registry. Set to false when the desired managed entry is retained for governance visibility after loading or reconciliation failed.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public MarketplaceInfo(
        String name,
        String source,
        Boolean isDefault,
        Boolean managed,
        Boolean available
    ) {
        this(name, source, isDefault, managed, available, null);
    }
}
