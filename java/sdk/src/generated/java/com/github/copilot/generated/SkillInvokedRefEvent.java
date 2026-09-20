/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "skill.invoked_ref". Internal durable skill invocation receipt whose content resolves from an earlier inline skill event in the same session.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SkillInvokedRefEvent extends SessionEvent {

    @Override
    public String getType() { return "skill.invoked_ref"; }

    @JsonProperty("data")
    private SkillInvokedRefEventData data;

    public SkillInvokedRefEventData getData() { return data; }
    public void setData(SkillInvokedRefEventData data) { this.data = data; }

    /** Data payload for {@link SkillInvokedRefEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SkillInvokedRefEventData(
        /** Name of the invoked skill */
        @JsonProperty("name") String name,
        /** Projected chat-message count when the skill was invoked. Preserved from the inline event data when the authored body is deduplicated. */
        @JsonProperty("invokedAtTurn") Long invokedAtTurn,
        /** Model identifier active when the skill was invoked, when known */
        @JsonProperty("model") String model,
        /** File path to the SKILL.md definition, or an empty string for an SDK-provided skill without a filesystem identity */
        @JsonProperty("path") String path,
        /** Content identifier of an earlier inline skill event in this session, in the prefixed form `sha256:<lowercase hex digest>` over the UTF-8 bytes of that event's `content` */
        @JsonProperty("contentId") String contentId,
        /** UTF-16 code unit length of the referenced skill content. Derived from the referenced body and validated against it when the reference is expanded; a reference whose length disagrees with the body it names is rejected instead of expanded */
        @JsonProperty("contentLength") Long contentLength,
        /** Tool names that should be auto-approved when this skill is active */
        @JsonProperty("allowedTools") List<String> allowedTools,
        /** Whether model invocation is disabled for this skill */
        @JsonProperty("disableModelInvocation") Boolean disableModelInvocation,
        /** Source identifier for where the skill was discovered */
        @JsonProperty("source") String source,
        /** Name of the plugin this skill originated from, when applicable */
        @JsonProperty("pluginName") String pluginName,
        /** Version of the plugin this skill originated from, when applicable */
        @JsonProperty("pluginVersion") String pluginVersion,
        /** Description of the skill from its SKILL.md frontmatter */
        @JsonProperty("description") String description,
        /** What triggered the skill invocation */
        @JsonProperty("trigger") SkillInvokedTrigger trigger
    ) {
    }
}
