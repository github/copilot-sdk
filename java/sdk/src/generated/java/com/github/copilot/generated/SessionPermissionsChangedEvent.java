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
 * Session event "session.permissions_changed". Permission-mode transition details.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionPermissionsChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.permissions_changed"; }

    @JsonProperty("data")
    private SessionPermissionsChangedEventData data;

    public SessionPermissionsChangedEventData getData() { return data; }
    public void setData(SessionPermissionsChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionPermissionsChangedEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionPermissionsChangedEventData(
        /** Permission mode before the change */
        @CopilotExperimental
        @JsonProperty("previousMode") PermissionMode previousMode,
        /** Permission mode after the change */
        @CopilotExperimental
        @JsonProperty("mode") PermissionMode mode,
        /** Explicit LLM judge model override used by assisted mode; omitted when the provider default applies */
        @CopilotExperimental
        @JsonProperty("assistedApprovalModel") String assistedApprovalModel
    ) {
    }
}
