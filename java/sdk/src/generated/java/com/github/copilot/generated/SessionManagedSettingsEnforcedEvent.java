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
 * Session event "session.managed_settings_enforced". Runtime enforcement of enterprise managed settings: fires when the session blocks or caps a runtime action because enterprise policy governs it, so SDK clients can explain *why* an action was governed. Unlike `session.managed_settings_resolved` (which reports *what* is managed), this reports a concrete governed action — e.g. a user or host tried to turn on bypass permissions or Assisted Permissions while the corresponding policy disables it. Emitted live (not persisted to the session event log) on user/host-initiated attempts only, never for silent policy application. Marked experimental while the managed-settings surface stabilizes.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionManagedSettingsEnforcedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.managed_settings_enforced"; }

    @JsonProperty("data")
    private SessionManagedSettingsEnforcedEventData data;

    public SessionManagedSettingsEnforcedEventData getData() { return data; }
    public void setData(SessionManagedSettingsEnforcedEventData data) { this.data = data; }

    /** Data payload for {@link SessionManagedSettingsEnforcedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionManagedSettingsEnforcedEventData(
        /** The category of runtime action that managed policy governed. */
        @JsonProperty("action") ManagedSettingsEnforcedAction action,
        /** For a `bypass_permissions_blocked` action, which permission-mode or escalation primitive was refused. Absent for actions without a specific primitive. */
        @JsonProperty("escalation") ManagedSettingsEnforcedEscalation escalation,
        /** The managed setting key responsible for the enforcement (for example `permissions.disableBypassPermissionsMode` or `permissions.disableAssistedPermissionsMode`). */
        @JsonProperty("setting") String setting,
        /** Whether the enforcement was forced by fail-closed handling (managed policy could not be determined) rather than an explicit managed setting. When true, `setting` still names the restriction that was applied. */
        @JsonProperty("failClosed") Boolean failClosed,
        /** A human-readable explanation of why the action was governed, suitable for surfacing to the user. */
        @JsonProperty("message") String message
    ) {
    }
}
