/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Session event "skill.context_delivered_ref". Internal durable receipt that reconstructs exact model-visible skill context from earlier session content.
 *
 * @apiNote This event type is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SkillContextDeliveredRefEvent extends SessionEvent {

    @Override
    public String getType() { return "skill.context_delivered_ref"; }

    @JsonProperty("data")
    private SkillContextDeliveredRefEventData data;

    public SkillContextDeliveredRefEventData getData() { return data; }
    public void setData(SkillContextDeliveredRefEventData data) { this.data = data; }

    /** Data payload for {@link SkillContextDeliveredRefEvent}. */
    @CopilotExperimental
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SkillContextDeliveredRefEventData(
        /** Content identifier of an earlier inline skill event in this session, in the prefixed form `sha256:<lowercase hex digest>` over the UTF-8 bytes of that event's `content` */
        @JsonProperty("contentId") String contentId,
        /** Exact text preceding the referenced content in the delivered wrapper */
        @JsonProperty("prefix") String prefix,
        /** Exact text following the referenced content in the delivered wrapper */
        @JsonProperty("suffix") String suffix,
        /** Unmodified injection provenance, in the form skill-<invocation-name> */
        @JsonProperty("source") String source,
        /** Interaction that delivered this context, when known */
        @JsonProperty("interactionId") String interactionId
    ) {
    }
}
