/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Privacy-safe reference to a permission-protected action that remains blocked for the current Autopilot epoch
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record BlockedPermissionAction(
    /** Permission-recovery episode that owns this blocked action */
    @JsonProperty("episodeId") String episodeId,
    /** Controlled permission request kind for the blocked action */
    @JsonProperty("permissionKind") String permissionKind,
    /** Recovery transition that made the action binding for the epoch */
    @JsonProperty("recoveryReason") PermissionRecoveryReason recoveryReason,
    /** Tool call that produced the final blocked permission request. Consumers can join this identifier to labeled permission and tool events for user-facing details. */
    @JsonProperty("toolCallId") String toolCallId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public BlockedPermissionAction {}

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
        private String episodeId;
        private String permissionKind;
        private PermissionRecoveryReason recoveryReason;
        private String toolCallId;

        /**
         * Sets the {@code episodeId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder episodeId(String value) {
            this.episodeId = value;
            return this;
        }

        /**
         * Sets the {@code permissionKind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder permissionKind(String value) {
            this.permissionKind = value;
            return this;
        }

        /**
         * Sets the {@code recoveryReason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder recoveryReason(PermissionRecoveryReason value) {
            this.recoveryReason = value;
            return this;
        }

        /**
         * Sets the {@code toolCallId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder toolCallId(String value) {
            this.toolCallId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public BlockedPermissionAction build() {
            if (episodeId == null) throw new IllegalStateException("Missing required property: episodeId");
            if (permissionKind == null) throw new IllegalStateException("Missing required property: permissionKind");
            if (recoveryReason == null) throw new IllegalStateException("Missing required property: recoveryReason");
            return new BlockedPermissionAction(episodeId, permissionKind, recoveryReason, toolCallId);
        }
    }
}
