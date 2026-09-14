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
 * Session event "skill.context_delivered". Exact skill context delivered to the model during a tool phase. This is not a user submission or another skill invocation.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SkillContextDeliveredEvent extends SessionEvent {

    @Override
    public String getType() { return "skill.context_delivered"; }

    @JsonProperty("data")
    private SkillContextDeliveredEventData data;

    public SkillContextDeliveredEventData getData() { return data; }
    public void setData(SkillContextDeliveredEventData data) { this.data = data; }

    /** Data payload for {@link SkillContextDeliveredEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SkillContextDeliveredEventData(
        /** Exact model-facing skill wrapper, including its invocation-time file context */
        @JsonProperty("content") String content,
        /** Unmodified injection provenance, in the form skill-<invocation-name> */
        @JsonProperty("source") String source,
        /** Interaction that delivered this context, when known */
        @JsonProperty("interactionId") String interactionId
    ) {
    }
}
