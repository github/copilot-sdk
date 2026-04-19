/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * The {@code system.message} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SystemMessageEvent extends SessionEvent {

    @Override
    public String getType() { return "system.message"; }

    @JsonProperty("data")
    private SystemMessageEventData data;

    public SystemMessageEventData getData() { return data; }
    public void setData(SystemMessageEventData data) { this.data = data; }

    /** Data payload for {@link SystemMessageEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SystemMessageEventData(
        /** The system or developer prompt text */
        @JsonProperty("content") String content,
        /** Message role: "system" for system prompts, "developer" for developer-injected instructions */
        @JsonProperty("role") SystemMessageEventDataRole role,
        /** Optional name identifier for the message source */
        @JsonProperty("name") String name,
        /** Metadata about the prompt template and its construction */
        @JsonProperty("metadata") SystemMessageEventDataMetadata metadata
    ) {

        /** Message role: "system" for system prompts, "developer" for developer-injected instructions */
        public enum SystemMessageEventDataRole {
            /** The {@code system} variant. */
            SYSTEM("system"),
            /** The {@code developer} variant. */
            DEVELOPER("developer");

            private final String value;
            SystemMessageEventDataRole(String value) { this.value = value; }
            @com.fasterxml.jackson.annotation.JsonValue
            public String getValue() { return value; }
            @com.fasterxml.jackson.annotation.JsonCreator
            public static SystemMessageEventDataRole fromValue(String value) {
                for (SystemMessageEventDataRole v : values()) {
                    if (v.value.equals(value)) return v;
                }
                throw new IllegalArgumentException("Unknown SystemMessageEventDataRole value: " + value);
            }
        }

        /** Metadata about the prompt template and its construction */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SystemMessageEventDataMetadata(
            /** Version identifier of the prompt template used */
            @JsonProperty("promptVersion") String promptVersion,
            /** Template variables used when constructing the prompt */
            @JsonProperty("variables") Map<String, Object> variables
        ) {
        }
    }
}
