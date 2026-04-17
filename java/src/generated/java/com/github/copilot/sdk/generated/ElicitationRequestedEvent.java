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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * The {@code elicitation.requested} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ElicitationRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "elicitation.requested"; }

    @JsonProperty("data")
    private ElicitationRequestedEventData data;

    public ElicitationRequestedEventData getData() { return data; }
    public void setData(ElicitationRequestedEventData data) { this.data = data; }

    /** Data payload for {@link ElicitationRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ElicitationRequestedEventData(
        /** Unique identifier for this elicitation request; used to respond via session.respondToElicitation() */
        @JsonProperty("requestId") String requestId,
        /** Tool call ID from the LLM completion; used to correlate with CompletionChunk.toolCall.id for remote UIs */
        @JsonProperty("toolCallId") String toolCallId,
        /** The source that initiated the request (MCP server name, or absent for agent-initiated) */
        @JsonProperty("elicitationSource") String elicitationSource,
        /** Message describing what information is needed from the user */
        @JsonProperty("message") String message,
        /** Elicitation mode; "form" for structured input, "url" for browser-based. Defaults to "form" when absent. */
        @JsonProperty("mode") ElicitationRequestedEventDataMode mode,
        /** JSON Schema describing the form fields to present to the user (form mode only) */
        @JsonProperty("requestedSchema") ElicitationRequestedEventDataRequestedSchema requestedSchema,
        /** URL to open in the user's browser (url mode only) */
        @JsonProperty("url") String url
    ) {

        /** Elicitation mode; "form" for structured input, "url" for browser-based. Defaults to "form" when absent. */
        public enum ElicitationRequestedEventDataMode {
            /** The {@code form} variant. */
            FORM("form"),
            /** The {@code url} variant. */
            URL("url");

            private final String value;
            ElicitationRequestedEventDataMode(String value) { this.value = value; }
            @com.fasterxml.jackson.annotation.JsonValue
            public String getValue() { return value; }
            @com.fasterxml.jackson.annotation.JsonCreator
            public static ElicitationRequestedEventDataMode fromValue(String value) {
                for (ElicitationRequestedEventDataMode v : values()) {
                    if (v.value.equals(value)) return v;
                }
                throw new IllegalArgumentException("Unknown ElicitationRequestedEventDataMode value: " + value);
            }
        }

        /** JSON Schema describing the form fields to present to the user (form mode only) */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record ElicitationRequestedEventDataRequestedSchema(
            /** Schema type indicator (always 'object') */
            @JsonProperty("type") String type,
            /** Form field definitions, keyed by field name */
            @JsonProperty("properties") Map<String, Object> properties,
            /** List of required field names */
            @JsonProperty("required") List<String> required
        ) {
        }
    }
}
