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
 * The policy questions a host asks before granting elevated permissions.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record PermissionsEvaluateManagedPolicyResult(
    /** True when managed policy forbids bypass-permissions mode, so allow-all style flags must not take effect. */
    @JsonProperty("bypassPermissionsDisabled") Boolean bypassPermissionsDisabled,
    /** True when a managed policy exists but this build could not determine its meaning. Hosts fail closed on it rather than assuming permission was granted. */
    @JsonProperty("policyUndetermined") Boolean policyUndetermined,
    /** True when the managed layer supplies permission rules that the session must enforce. */
    @JsonProperty("permissionRulesActive") Boolean permissionRulesActive
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    PermissionsEvaluateManagedPolicyResult {}

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
        private Boolean bypassPermissionsDisabled;
        private Boolean policyUndetermined;
        private Boolean permissionRulesActive;

        /**
         * Sets the {@code bypassPermissionsDisabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder bypassPermissionsDisabled(Boolean value) {
            this.bypassPermissionsDisabled = value;
            return this;
        }

        /**
         * Sets the {@code policyUndetermined} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder policyUndetermined(Boolean value) {
            this.policyUndetermined = value;
            return this;
        }

        /**
         * Sets the {@code permissionRulesActive} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder permissionRulesActive(Boolean value) {
            this.permissionRulesActive = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PermissionsEvaluateManagedPolicyResult build() {
            if (bypassPermissionsDisabled == null) throw new IllegalStateException("Missing required property: bypassPermissionsDisabled");
            if (policyUndetermined == null) throw new IllegalStateException("Missing required property: policyUndetermined");
            if (permissionRulesActive == null) throw new IllegalStateException("Missing required property: permissionRulesActive");
            return new PermissionsEvaluateManagedPolicyResult(bypassPermissionsDisabled, policyUndetermined, permissionRulesActive);
        }
    }
}
