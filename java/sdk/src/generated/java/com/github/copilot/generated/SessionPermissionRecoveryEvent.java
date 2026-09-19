/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "session.permission_recovery". Authoritative snapshot of an Autopilot permission-recovery episode
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionPermissionRecoveryEvent extends SessionEvent {

    @Override
    public String getType() { return "session.permission_recovery"; }

    @JsonProperty("data")
    private SessionPermissionRecoveryEventData data;

    public SessionPermissionRecoveryEventData getData() { return data; }
    public void setData(SessionPermissionRecoveryEventData data) { this.data = data; }

    /** Data payload for {@link SessionPermissionRecoveryEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionPermissionRecoveryEventData(
        /** Stable identifier shared by every transition in this recovery episode */
        @JsonProperty("episodeId") String episodeId,
        /** Current lifecycle state of the recovery episode */
        @JsonProperty("status") PermissionRecoveryStatus status,
        /** Policy selected from the current client's response capability; mode or client changes may update it during recovery */
        @JsonProperty("onBlocked") PermissionRecoveryOnBlocked onBlocked,
        /** Controlled reason for the latest episode transition */
        @JsonProperty("reason") PermissionRecoveryReason reason,
        /** Maximum number of distinct autonomous permission attempts allowed before escalation */
        @JsonProperty("maxAttempts") Long maxAttempts,
        /** Ordered privacy-safe record of permission attempts and the successful alternative, when any */
        @JsonProperty("attempts") List<PermissionRecoveryAttempt> attempts
    ) {
    }
}
