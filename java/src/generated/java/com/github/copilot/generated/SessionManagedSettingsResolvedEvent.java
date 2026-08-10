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
 * Session event "session.managed_settings_resolved". Enterprise managed-settings resolution: the effective managed settings the session applied and which channels contributed, so SDK clients can show users what is enterprise-managed. Fires whenever managed policy is (re)applied — at session start, on resume, and on account switch. This is an ephemeral live snapshot (delivered to subscribers but not persisted to the session event log), because at session start it resolves before `session.start` is emitted. Device values take precedence over server values per ordinary key, while permissions compose restrictively across device, server, and SDK-client layers. The account-scoped `getManagedSettings()` API does not include session-local client injection. Marked experimental while the managed-settings surface stabilizes.
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
        /** Channel summary: `server`, `device`, or `client` when exactly one channel contributed; `mixed` when multiple channels contributed; otherwise `none`. Consult the per-channel booleans for exact provenance. */
        @JsonProperty("source") ManagedSettingsResolvedSource source,
        /** Whether the server (account/org) managed-settings layer was present */
        @JsonProperty("serverManaged") Boolean serverManaged,
        /** Whether an actual device MDM/plist/registry/file managed-settings layer was present */
        @JsonProperty("deviceManaged") Boolean deviceManaged,
        /** Whether a session-local permissions layer injected by the SDK host was present */
        @JsonProperty("clientManaged") Boolean clientManaged,
        /** Whether managed policy could not be determined (e.g. a failed server fetch) and the session fell back to the fail-closed restriction. When true, restrictions such as disabling bypass-permissions are enforced even though `settings` may be absent. */
        @JsonProperty("failClosed") Boolean failClosed,
        /** Whether enterprise policy disables bypass-permissions ("yolo") mode for this session. Deny-wins across layers, and forced on when `failClosed` is true. */
        @JsonProperty("bypassPermissionsDisabled") Boolean bypassPermissionsDisabled,
        /** Whether at least two managed sources supplied permission allowlists, so enforcement intersects them and the flattened settings payload omits `permissions.allow`. */
        @JsonProperty("permissionsAllowIntersected") Boolean permissionsAllowIntersected,
        /** The setting keys under enterprise management in the effective managed settings (e.g. `model`, `enabledPlugins`, `permissions`). Empty when no managed settings are in force. */
        @JsonProperty("managedKeys") List<String> managedKeys,
        /** The effective (resolved) managed settings values, so clients can render exactly what is enforced. Absent when no managed policy is in force. */
        @JsonProperty("settings") Object settings
    ) {
    }
}
