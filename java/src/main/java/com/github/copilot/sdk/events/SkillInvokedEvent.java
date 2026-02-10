/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Event: skill.invoked
 * <p>
 * This event is emitted when a skill is invoked during a session.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SkillInvokedEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private SkillInvokedData data;

    @Override
    public String getType() {
        return "skill.invoked";
    }

    public SkillInvokedData getData() {
        return data;
    }

    public void setData(SkillInvokedData data) {
        this.data = data;
    }

    /**
     * Data for the skill invoked event.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SkillInvokedData(@JsonProperty("name") String name, @JsonProperty("path") String path,
            @JsonProperty("content") String content, @JsonProperty("allowedTools") List<String> allowedTools) {
    }
}
