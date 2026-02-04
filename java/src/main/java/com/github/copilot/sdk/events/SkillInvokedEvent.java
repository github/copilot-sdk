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
    public static class SkillInvokedData {

        @JsonProperty("name")
        private String name;

        @JsonProperty("path")
        private String path;

        @JsonProperty("content")
        private String content;

        @JsonProperty("allowedTools")
        private List<String> allowedTools;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public List<String> getAllowedTools() {
            return allowedTools;
        }

        public void setAllowedTools(List<String> allowedTools) {
            this.allowedTools = allowedTools;
        }
    }
}
