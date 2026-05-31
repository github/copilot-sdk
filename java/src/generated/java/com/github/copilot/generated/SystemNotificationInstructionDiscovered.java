/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Schema for the `SystemNotificationInstructionDiscovered` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SystemNotificationInstructionDiscovered extends SystemNotification {

    @JsonProperty("type")
    private final String type = "instruction_discovered";

    @Override
    public String getType() { return type; }

    /** Relative path to the discovered instruction file */
    @JsonProperty("sourcePath")
    private String sourcePath;

    /** Path of the file access that triggered discovery */
    @JsonProperty("triggerFile")
    private String triggerFile;

    /** Tool command that triggered discovery (currently always 'view') */
    @JsonProperty("triggerTool")
    private String triggerTool;

    /** Human-readable label for the timeline (e.g., 'AGENTS.md from packages/billing/') */
    @JsonProperty("description")
    private String description;

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

    public String getTriggerFile() { return triggerFile; }
    public void setTriggerFile(String triggerFile) { this.triggerFile = triggerFile; }

    public String getTriggerTool() { return triggerTool; }
    public void setTriggerTool(String triggerTool) { this.triggerTool = triggerTool; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
