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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "session.managed_settings_resolved". Effective enterprise managed settings applied to the session and their contributing channels. Emitted whenever managed policy is applied or reapplied, including session start, resume, and account switch. This ephemeral live snapshot is delivered to subscribers but not persisted to the session event log; initial resolution occurs before session.start.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
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
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionManagedSettingsResolvedEventData(
        /** Channel summary: `server`, `device`, `client`, or `policyHelper` when exactly one channel contributed; `mixed` when multiple channels contributed; otherwise `none`. Consult the per-channel booleans for exact provenance. */
        @JsonProperty("source") ManagedSettingsResolvedSource source,
        /** Whether the server (account/org) managed-settings layer was present */
        @JsonProperty("serverManaged") Boolean serverManaged,
        /** Whether an actual device MDM/plist/registry/file managed-settings layer was present */
        @JsonProperty("deviceManaged") Boolean deviceManaged,
        /** Whether a session-local permissions layer injected by the SDK host was present */
        @JsonProperty("clientManaged") Boolean clientManaged,
        /** Whether the policy-helper managed-settings layer was present. The policy helper is the weakest channel: it fills keys no enterprise source set and can never replace one. */
        @JsonProperty("policyHelperManaged") Boolean policyHelperManaged,
        /** Whether managed policy could not be determined (e.g. a failed server fetch) and the session fell back to the fail-closed restriction. When true, restrictions such as disabling bypass-permissions are enforced even though `settings` may be absent. */
        @JsonProperty("failClosed") Boolean failClosed,
        /** Whether the effective sandbox policy forces the sandbox on *only* because managed policy could not be determined, rather than because the policy requires it. Lets clients tell a user whose `--no-sandbox` was overridden that the sandbox stayed on as a fail-closed fallback, instead of attributing it to an administrator who set no such policy. */
        @JsonProperty("sandboxEnabledByUndeterminedPolicy") Boolean sandboxEnabledByUndeterminedPolicy,
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
