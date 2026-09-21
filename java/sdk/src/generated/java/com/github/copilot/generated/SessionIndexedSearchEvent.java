/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import javax.annotation.processing.Generated;

/**
 * Session event "session.indexed_search". Transient indexed-search status and diagnostics from the live runtime service. Never persisted or used to infer activation from session history.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionIndexedSearchEvent extends SessionEvent {

    @Override
    public String getType() { return "session.indexed_search"; }

    @JsonProperty("data")
    private SessionIndexedSearchEventData data;

    public SessionIndexedSearchEventData getData() { return data; }
    public void setData(SessionIndexedSearchEventData data) { this.data = data; }

    /** Raw union payload for {@link SessionIndexedSearchEvent}, preserving every variant's fields. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionIndexedSearchEventData(JsonNode raw) {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public SessionIndexedSearchEventData {}

        public SessionIndexedSearchEventData() {
            this(JsonNodeFactory.instance.objectNode());
        }

        @JsonValue
        public JsonNode raw() { return raw; }
    }
}
