/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Enterprise permission policy expressed with the runtime's managed permission-rule syntax.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionManagedPermissions(
    /** When set to `disable`, prevents bypass/allow-all permission modes. Advisory auto-approval remains available because normal prompt paths stay active. Any other value is accepted rather than failing the session, but is enforced as `disable`: the key is only present to restrict something, so a mode this runtime cannot interpret fails closed to the most restrictive one it knows. Omit the key entirely to impose no restriction. */
    @JsonProperty("disableBypassPermissionsMode") String disableBypassPermissionsMode,
    /** When true, prevents Assisted Permissions from being activated. An actively Assisted session falls back to Manual Approval while the policy is in force. Omit the key or set it to false to impose no restriction. */
    @JsonProperty("disableAssistedPermissionsMode") Boolean disableAssistedPermissionsMode,
    /** Permission rules that block matching operations. Deny has highest precedence. */
    @JsonProperty("deny") List<String> deny,
    /** Permission rules that require explicit human approval. */
    @JsonProperty("ask") List<String> ask,
    /** Permission rules that allow matching operations unless another managed source, deny, or ask rule restricts them. */
    @JsonProperty("allow") List<String> allow,
    /** Closed-world host boundary expressed as `Domain(hostname)`, `Domain(IP)`, or `Domain(*.example.com)` rules. Schemes, ports, paths, queries, and fragments are rejected because every network request must be enforceable at host-level egress. Multiple managed sources intersect their lists; an empty list denies all hosts. */
    @JsonProperty("limitTo") List<String> limitTo
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionManagedPermissions {}

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
        private String disableBypassPermissionsMode;
        private Boolean disableAssistedPermissionsMode;
        private List<String> deny;
        private List<String> ask;
        private List<String> allow;
        private List<String> limitTo;

        /**
         * Sets the {@code disableBypassPermissionsMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder disableBypassPermissionsMode(String value) {
            this.disableBypassPermissionsMode = value;
            return this;
        }

        /**
         * Sets the {@code disableAssistedPermissionsMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder disableAssistedPermissionsMode(Boolean value) {
            this.disableAssistedPermissionsMode = value;
            return this;
        }

        /**
         * Sets the {@code deny} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder deny(List<String> value) {
            this.deny = value;
            return this;
        }

        /**
         * Sets the {@code ask} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder ask(List<String> value) {
            this.ask = value;
            return this;
        }

        /**
         * Sets the {@code allow} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder allow(List<String> value) {
            this.allow = value;
            return this;
        }

        /**
         * Sets the {@code limitTo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder limitTo(List<String> value) {
            this.limitTo = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionManagedPermissions build() {
            return new SessionManagedPermissions(disableBypassPermissionsMode, disableAssistedPermissionsMode, deny, ask, allow, limitTo);
        }
    }
}
