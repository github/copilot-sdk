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
 * Session event "session.managed_settings_resolved". Enterprise managed-settings resolution: the effective managed settings the session applied and where they came from, so SDK clients can show users what is enterprise-managed and by which authority. Fires whenever managed policy is (re)applied — at session start, on resume, and on account switch. This is an ephemeral live snapshot (delivered to subscribers but not persisted to the session event log), because at session start it resolves before `session.start` is emitted; for a session-independent pull, use the SDK `getManagedSettings()` API, which returns the identical payload. Managed settings have a single authoritative source, so the highest-authority present layer (server > device) wins wholesale; `bypassPermissionsDisabled` is deny-wins across layers. Marked experimental while the managed-settings surface stabilizes.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionManagedSettingsResolvedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.managed_settings_resolved"; }

    @JsonProperty("data")
    private SessionManagedSettingsResolvedEventData data;

    public SessionManagedSettingsResolvedEventData getData() { return data; }
    public void setData(SessionManagedSettingsResolvedEventData data) { this.data = data; }

    /** Data payload for {@link SessionManagedSettingsResolvedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionManagedSettingsResolvedEventData(
        /** Which channel supplied the effective managed settings (the winning layer), or `none` when no policy is in force */
        @JsonProperty("source") ManagedSettingsResolvedSource source,
        /** Whether the server (account/org) managed-settings layer was present */
        @JsonProperty("serverManaged") Boolean serverManaged,
        /** Whether the device (MDM/plist/registry/file) managed-settings layer was present */
        @JsonProperty("deviceManaged") Boolean deviceManaged,
        /** Whether managed policy could not be determined (e.g. a failed server fetch) and the session fell back to the fail-closed restriction. When true, restrictions such as disabling bypass-permissions are enforced even though `settings` may be absent. */
        @JsonProperty("failClosed") Boolean failClosed,
        /** Whether enterprise policy disables bypass-permissions ("yolo") mode for this session. Deny-wins across layers, and forced on when `failClosed` is true. */
        @JsonProperty("bypassPermissionsDisabled") Boolean bypassPermissionsDisabled,
        /** The setting keys under enterprise management in the effective managed settings (e.g. `model`, `enabledPlugins`, `permissions`). Empty when no managed settings are in force. */
        @JsonProperty("managedKeys") List<String> managedKeys,
        /** The effective (resolved) managed settings values, so clients can render exactly what is enforced. Absent when no managed policy is in force. */
        @JsonProperty("settings") Object settings
    ) {
    }
}
