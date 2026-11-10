/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Fallback for event types not yet known to this SDK version.
 * <p>
 * {@link #getType()} returns the original type string from the JSON payload,
 * preserving forward compatibility with event types introduced by newer CLI versions.
 * Unrecognized fields, including the event payload, are preserved when serialized.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class UnknownSessionEvent extends SessionEvent {

    @JsonProperty("type")
    private String type = "unknown";

    private final Map<String, JsonNode> additionalProperties = new LinkedHashMap<>();

    @JsonAnySetter
    public void setAdditionalProperty(String name, JsonNode value) {
        additionalProperties.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, JsonNode> getAdditionalProperties() { return additionalProperties; }

    @Override
    public String getType() { return type; }
}
