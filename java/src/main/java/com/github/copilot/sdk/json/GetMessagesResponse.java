package com.github.copilot.sdk.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Internal response object from getting session messages.
 *
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class GetMessagesResponse {

    @JsonProperty("events")
    private List<JsonNode> events;

    public List<JsonNode> getEvents() {
        return events;
    }

    public void setEvents(List<JsonNode> events) {
        this.events = events;
    }
}
