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
 * The {@code skill.invoked} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SkillInvokedEvent extends SessionEvent {

    @Override
    public String getType() { return "skill.invoked"; }

    @JsonProperty("data")
    private SkillInvokedEventData data;

    public SkillInvokedEventData getData() { return data; }
    public void setData(SkillInvokedEventData data) { this.data = data; }

    /** Data payload for {@link SkillInvokedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SkillInvokedEventData(
        /** Name of the invoked skill */
        @JsonProperty("name") String name,
        /** File path to the SKILL.md definition */
        @JsonProperty("path") String path,
        /** Full content of the skill file, injected into the conversation for the model */
        @JsonProperty("content") String content,
        /** Tool names that should be auto-approved when this skill is active */
        @JsonProperty("allowedTools") List<String> allowedTools,
        /** Name of the plugin this skill originated from, when applicable */
        @JsonProperty("pluginName") String pluginName,
        /** Version of the plugin this skill originated from, when applicable */
        @JsonProperty("pluginVersion") String pluginVersion,
        /** Description of the skill from its SKILL.md frontmatter */
        @JsonProperty("description") String description
    ) {
    }
}
