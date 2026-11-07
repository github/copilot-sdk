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
 * Available permission modes, derived from current session policy.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPermissionsGetCapabilitiesResult(
    /** Whether LLM-assisted approval is enabled for this session. */
    @JsonProperty("assistedApproval") Boolean assistedApproval,
    /** Whether managed policy currently permits unconditional approval.
Stays true while managed policy is still resolving. */
    @JsonProperty("bypassPermissions") Boolean bypassPermissions
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionPermissionsGetCapabilitiesResult {}

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
        private Boolean assistedApproval;
        private Boolean bypassPermissions;

        /**
         * Sets the {@code assistedApproval} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder assistedApproval(Boolean value) {
            this.assistedApproval = value;
            return this;
        }

        /**
         * Sets the {@code bypassPermissions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder bypassPermissions(Boolean value) {
            this.bypassPermissions = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionPermissionsGetCapabilitiesResult build() {
            if (assistedApproval == null) throw new IllegalStateException("Missing required property: assistedApproval");
            if (bypassPermissions == null) throw new IllegalStateException("Missing required property: bypassPermissions");
            return new SessionPermissionsGetCapabilitiesResult(assistedApproval, bypassPermissions);
        }
    }
}
