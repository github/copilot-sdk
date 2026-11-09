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
 * Effective permission escalation and the original launch baseline.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPermissionsGetAllowAllResult(
    /** Whether allow-all permissions are currently effective. */
    @JsonProperty("active") Boolean active,
    /** Whether automatic permission approval is active. */
    @JsonProperty("autoApproval") Boolean autoApproval,
    /** Whether automatic permission approval is stored before the managed-policy cap. */
    @JsonProperty("storedAutoApproval") Boolean storedAutoApproval,
    /** Whether an invocation changed the launch permission posture. */
    @JsonProperty("runtimeOverride") Boolean runtimeOverride,
    /** Original launch permissions, unchanged by runtime escalation. */
    @JsonProperty("baseline") PermissionsAllowAllBaseline baseline
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionPermissionsGetAllowAllResult {}

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
        private Boolean active;
        private Boolean autoApproval;
        private Boolean storedAutoApproval;
        private Boolean runtimeOverride;
        private PermissionsAllowAllBaseline baseline;

        /**
         * Sets the {@code active} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder active(Boolean value) {
            this.active = value;
            return this;
        }

        /**
         * Sets the {@code autoApproval} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder autoApproval(Boolean value) {
            this.autoApproval = value;
            return this;
        }

        /**
         * Sets the {@code storedAutoApproval} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder storedAutoApproval(Boolean value) {
            this.storedAutoApproval = value;
            return this;
        }

        /**
         * Sets the {@code runtimeOverride} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder runtimeOverride(Boolean value) {
            this.runtimeOverride = value;
            return this;
        }

        /**
         * Sets the {@code baseline} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder baseline(PermissionsAllowAllBaseline value) {
            this.baseline = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionPermissionsGetAllowAllResult build() {
            if (active == null) throw new IllegalStateException("Missing required property: active");
            if (autoApproval == null) throw new IllegalStateException("Missing required property: autoApproval");
            if (storedAutoApproval == null) throw new IllegalStateException("Missing required property: storedAutoApproval");
            if (runtimeOverride == null) throw new IllegalStateException("Missing required property: runtimeOverride");
            if (baseline == null) throw new IllegalStateException("Missing required property: baseline");
            return new SessionPermissionsGetAllowAllResult(active, autoApproval, storedAutoApproval, runtimeOverride, baseline);
        }
    }
}
