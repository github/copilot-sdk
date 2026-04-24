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
 * The {@code session.skills_loaded} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionSkillsLoadedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.skills_loaded"; }

    @JsonProperty("data")
    private SessionSkillsLoadedEventData data;

    public SessionSkillsLoadedEventData getData() { return data; }
    public void setData(SessionSkillsLoadedEventData data) { this.data = data; }

    /** Data payload for {@link SessionSkillsLoadedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionSkillsLoadedEventData(
        /** Array of resolved skill metadata */
        @JsonProperty("skills") List<SkillsLoadedSkill> skills
    ) {
    }
}
