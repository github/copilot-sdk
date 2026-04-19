/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The {@code system.notification} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SystemNotificationEvent extends SessionEvent {

    @Override
    public String getType() { return "system.notification"; }

    @JsonProperty("data")
    private SystemNotificationEventData data;

    public SystemNotificationEventData getData() { return data; }
    public void setData(SystemNotificationEventData data) { this.data = data; }

    /** Data payload for {@link SystemNotificationEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SystemNotificationEventData(
        /** The notification text, typically wrapped in <system_notification> XML tags */
        @JsonProperty("content") String content,
        /** Structured metadata identifying what triggered this notification */
        @JsonProperty("kind") Object kind
    ) {
    }
}
