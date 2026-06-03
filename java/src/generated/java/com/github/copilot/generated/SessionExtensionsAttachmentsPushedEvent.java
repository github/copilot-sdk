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
 * Session event "session.extensions.attachments_pushed".
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionExtensionsAttachmentsPushedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.extensions.attachments_pushed"; }

    @JsonProperty("data")
    private SessionExtensionsAttachmentsPushedEventData data;

    public SessionExtensionsAttachmentsPushedEventData getData() { return data; }
    public void setData(SessionExtensionsAttachmentsPushedEventData data) { this.data = data; }

    /** Data payload for {@link SessionExtensionsAttachmentsPushedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionExtensionsAttachmentsPushedEventData(
        /** Attachments contributed by an extension; the host should surface these as composer pills and forward them via the next session.send call. */
        @JsonProperty("attachments") List<Object> attachments
    ) {
    }
}
