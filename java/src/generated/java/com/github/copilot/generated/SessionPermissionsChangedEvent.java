/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "session.permissions_changed". Permissions change details carrying the aggregate allow-all boolean transition.
 * @since 1.0.0
 */
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionPermissionsChangedEventData(
        /** Aggregate allow-all flag before the change */
        @JsonProperty("previousAllowAllPermissions") Boolean previousAllowAllPermissions,
        /** Aggregate allow-all flag after the change */
        @JsonProperty("allowAllPermissions") Boolean allowAllPermissions
    ) {
    }
}
