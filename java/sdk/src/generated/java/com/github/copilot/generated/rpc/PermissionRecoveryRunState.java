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
 * Replay-safe epoch counters for Autopilot Assisted Permissions recovery
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionRecoveryRunState(
    /** Number of blocked recovery episodes since the latest successful permitted effectful tool call */
    @JsonProperty("consecutiveBlockedEpisodes") Long consecutiveBlockedEpisodes,
    /** Whether the claimed epoch terminal is an authoritative permission-recovery block that remains latched until the next genuine root user message */
    @JsonProperty("permissionRecoveryTerminalClaimed") Boolean permissionRecoveryTerminalClaimed,
    /** Total deferred, denied, or blocked attempt transitions in the current genuine-user-message epoch */
    @JsonProperty("totalDenialDispositions") Long totalDenialDispositions,
    /** Whether a permission-recovery terminal completion has already been claimed for the epoch */
    @JsonProperty("terminalOutcomeClaimed") Boolean terminalOutcomeClaimed
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PermissionRecoveryRunState {}

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
        private Long consecutiveBlockedEpisodes;
        private Boolean permissionRecoveryTerminalClaimed;
        private Long totalDenialDispositions;
        private Boolean terminalOutcomeClaimed;

        /**
         * Sets the {@code consecutiveBlockedEpisodes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder consecutiveBlockedEpisodes(Long value) {
            this.consecutiveBlockedEpisodes = value;
            return this;
        }

        /**
         * Sets the {@code permissionRecoveryTerminalClaimed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder permissionRecoveryTerminalClaimed(Boolean value) {
            this.permissionRecoveryTerminalClaimed = value;
            return this;
        }

        /**
         * Sets the {@code totalDenialDispositions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder totalDenialDispositions(Long value) {
            this.totalDenialDispositions = value;
            return this;
        }

        /**
         * Sets the {@code terminalOutcomeClaimed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder terminalOutcomeClaimed(Boolean value) {
            this.terminalOutcomeClaimed = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public PermissionRecoveryRunState build() {
            if (consecutiveBlockedEpisodes == null) throw new IllegalStateException("Missing required property: consecutiveBlockedEpisodes");
            if (totalDenialDispositions == null) throw new IllegalStateException("Missing required property: totalDenialDispositions");
            if (terminalOutcomeClaimed == null) throw new IllegalStateException("Missing required property: terminalOutcomeClaimed");
            return new PermissionRecoveryRunState(consecutiveBlockedEpisodes, permissionRecoveryTerminalClaimed, totalDenialDispositions, terminalOutcomeClaimed);
        }
    }

    /**
     * Creates a record with the components it had before later optional fields were added.
     * @deprecated Use named construction via {@link #builder()}.
     * @param consecutiveBlockedEpisodes Number of blocked recovery episodes since the latest successful permitted effectful tool call
     * @param totalDenialDispositions Total deferred, denied, or blocked attempt transitions in the current genuine-user-message epoch
     * @param terminalOutcomeClaimed Whether a permission-recovery terminal completion has already been claimed for the epoch
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public PermissionRecoveryRunState(
        Long consecutiveBlockedEpisodes,
        Long totalDenialDispositions,
        Boolean terminalOutcomeClaimed
    ) {
        this(consecutiveBlockedEpisodes, null, totalDenialDispositions, terminalOutcomeClaimed);
    }
}
