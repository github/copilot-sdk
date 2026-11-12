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
 * Session event "session.journal_link". Links a standalone replay head to its complete preceding numbered journal archive.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionJournalLinkEvent extends SessionEvent {

    @Override
    public String getType() { return "session.journal_link"; }

    @JsonProperty("data")
    private SessionJournalLinkEventData data;

    public SessionJournalLinkEventData getData() { return data; }
    public void setData(SessionJournalLinkEventData data) { this.data = data; }

    /** Data payload for {@link SessionJournalLinkEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionJournalLinkEventData(
        /** Journal checkpoint format version. */
        @JsonProperty("version") Long version,
        /** Session owning this journal chain. */
        @JsonProperty("sessionId") String sessionId,
        /** Numbered predecessor filename in the same session directory. */
        @JsonProperty("previousFile") String previousFile
    ) {
    }
}
