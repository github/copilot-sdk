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
 * Credential-free Connector catalog entry.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConnectorCatalogEntry(
    /** Canonical Connector name used by lifecycle methods. */
    @JsonProperty("name") String name,
    /** Untrusted display label from the service. */
    @JsonProperty("displayName") String displayName,
    /** Untrusted service description, when present. */
    @JsonProperty("description") String description,
    /** Optional catalog logo. */
    @JsonProperty("logo") String logo,
    /** Optional catalog tier. */
    @JsonProperty("tier") String tier,
    /** Optional catalog release tag. */
    @JsonProperty("releaseTag") String releaseTag,
    /** Current authoritative service connection state. */
    @JsonProperty("status") ConnectorCatalogStatus status,
    /** Opaque stable runtime IDs currently projected into the session for this Connector. */
    @JsonProperty("runtimeServerIds") List<String> runtimeServerIds
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ConnectorCatalogEntry {}

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
        private String name;
        private String displayName;
        private String description;
        private String logo;
        private String tier;
        private String releaseTag;
        private ConnectorCatalogStatus status;
        private List<String> runtimeServerIds;

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
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /**
         * Sets the {@code logo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder logo(String value) {
            this.logo = value;
            return this;
        }

        /**
         * Sets the {@code tier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder tier(String value) {
            this.tier = value;
            return this;
        }

        /**
         * Sets the {@code releaseTag} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder releaseTag(String value) {
            this.releaseTag = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder status(ConnectorCatalogStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code runtimeServerIds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder runtimeServerIds(List<String> value) {
            this.runtimeServerIds = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ConnectorCatalogEntry build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (displayName == null) throw new IllegalStateException("Missing required property: displayName");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            if (runtimeServerIds == null) throw new IllegalStateException("Missing required property: runtimeServerIds");
            return new ConnectorCatalogEntry(name, displayName, description, logo, tier, releaseTag, status, runtimeServerIds);
        }
    }

    /**
     * Creates a record with the components it had before later optional fields were added.
     * @deprecated Use named construction via {@link #builder()}.
     * @param name Canonical Connector name used by lifecycle methods.
     * @param displayName Untrusted display label from the service.
     * @param description Untrusted service description, when present.
     * @param status Current authoritative service connection state.
     * @param runtimeServerIds Opaque stable runtime IDs currently projected into the session for this Connector.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ConnectorCatalogEntry(
        String name,
        String displayName,
        String description,
        ConnectorCatalogStatus status,
        List<String> runtimeServerIds
    ) {
        this(name, displayName, description, null, null, null, status, runtimeServerIds);
    }
}
