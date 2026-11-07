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
 * Runtime support and session-context standalone executable extension policy.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionExtensionsGetPolicyResult(
    /** Whether standalone extensions are allowed, prohibited by plugin-only policy, or pending unresolved managed policy. */
    @JsonProperty("standalone") StandaloneExtensionAdmission standalone,
    /** Extension policy enforcement contract version. Version 1 includes standalone discovery, launch admission, and live-policy revocation. */
    @JsonProperty("enforcementVersion") Long enforcementVersion,
    /** Executable entries discovered from this session's runtime-verified eligible plugins. Under plugin_only, independently launched targets must match these entry paths; an external package, a caller-supplied plugin label, or a synthetic transport bootstrap does not establish eligibility. Empty while pending or when no extension controller is configured. */
    @JsonProperty("eligiblePluginEntries") List<EligiblePluginExtensionEntry> eligiblePluginEntries
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionExtensionsGetPolicyResult {}

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
        private StandaloneExtensionAdmission standalone;
        private Long enforcementVersion;
        private List<EligiblePluginExtensionEntry> eligiblePluginEntries;

        /**
         * Sets the {@code standalone} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder standalone(StandaloneExtensionAdmission value) {
            this.standalone = value;
            return this;
        }

        /**
         * Sets the {@code enforcementVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder enforcementVersion(Long value) {
            this.enforcementVersion = value;
            return this;
        }

        /**
         * Sets the {@code eligiblePluginEntries} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder eligiblePluginEntries(List<EligiblePluginExtensionEntry> value) {
            this.eligiblePluginEntries = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionExtensionsGetPolicyResult build() {
            if (standalone == null) throw new IllegalStateException("Missing required property: standalone");
            if (enforcementVersion == null) throw new IllegalStateException("Missing required property: enforcementVersion");
            if (eligiblePluginEntries == null) throw new IllegalStateException("Missing required property: eligiblePluginEntries");
            return new SessionExtensionsGetPolicyResult(standalone, enforcementVersion, eligiblePluginEntries);
        }
    }
}
