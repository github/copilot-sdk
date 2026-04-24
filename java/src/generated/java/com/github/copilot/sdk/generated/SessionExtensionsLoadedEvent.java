/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The {@code session.extensions_loaded} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionExtensionsLoadedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.extensions_loaded"; }

    @JsonProperty("data")
    private SessionExtensionsLoadedEventData data;

    public SessionExtensionsLoadedEventData getData() { return data; }
    public void setData(SessionExtensionsLoadedEventData data) { this.data = data; }

    /** Data payload for {@link SessionExtensionsLoadedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionExtensionsLoadedEventData(
        /** Array of discovered extensions and their status */
        @JsonProperty("extensions") List<ExtensionsLoadedExtension> extensions
    ) {
    }
}
