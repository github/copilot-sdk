/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "session.custom_notification". Opaque custom notification data. Consumers may branch on source and name, but payload semantics are source-defined.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionCustomNotificationEvent extends SessionEvent {

    @Override
    public String getType() { return "session.custom_notification"; }

    @JsonProperty("data")
    private SessionCustomNotificationEventData data;

    public SessionCustomNotificationEventData getData() { return data; }
    public void setData(SessionCustomNotificationEventData data) { this.data = data; }

    /** Data payload for {@link SessionCustomNotificationEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionCustomNotificationEventData(
        /** Namespace for the custom notification producer */
        @JsonProperty("source") String source,
        /** Source-defined custom notification name */
        @JsonProperty("name") String name,
        /** Optional source-defined payload schema version */
        @JsonProperty("version") Long version,
        /** Optional source-defined string identifiers describing the payload subject */
        @JsonProperty("subject") Map<String, String> subject,
        /** Source-defined JSON payload for the custom notification */
        @JsonProperty("payload") Object payload
    ) {
    }
}
