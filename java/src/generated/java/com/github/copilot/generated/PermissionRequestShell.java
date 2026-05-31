/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Shell command permission request
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionRequestShell extends PermissionRequest {

    @JsonProperty("kind")
    private final String kind = "shell";

    @Override
    public String getKind() { return kind; }

    /** Tool call ID that triggered this permission request */
    @JsonProperty("toolCallId")
    private String toolCallId;

    /** The complete shell command text to be executed */
    @JsonProperty("fullCommandText")
    private String fullCommandText;

    /** Human-readable description of what the command intends to do */
    @JsonProperty("intention")
    private String intention;

    /** Parsed command identifiers found in the command text */
    @JsonProperty("commands")
    private List<PermissionRequestShellCommand> commands;

    /** File paths that may be read or written by the command */
    @JsonProperty("possiblePaths")
    private List<String> possiblePaths;

    /** URLs that may be accessed by the command */
    @JsonProperty("possibleUrls")
    private List<PermissionRequestShellPossibleUrl> possibleUrls;

    /** Whether the command includes a file write redirection (e.g., > or >>) */
    @JsonProperty("hasWriteFileRedirection")
    private Boolean hasWriteFileRedirection;

    /** Whether the UI can offer session-wide approval for this command pattern */
    @JsonProperty("canOfferSessionApproval")
    private Boolean canOfferSessionApproval;

    /** Optional warning message about risks of running this command */
    @JsonProperty("warning")
    private String warning;

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    public String getFullCommandText() { return fullCommandText; }
    public void setFullCommandText(String fullCommandText) { this.fullCommandText = fullCommandText; }

    public String getIntention() { return intention; }
    public void setIntention(String intention) { this.intention = intention; }

    public List<PermissionRequestShellCommand> getCommands() { return commands; }
    public void setCommands(List<PermissionRequestShellCommand> commands) { this.commands = commands; }

    public List<String> getPossiblePaths() { return possiblePaths; }
    public void setPossiblePaths(List<String> possiblePaths) { this.possiblePaths = possiblePaths; }

    public List<PermissionRequestShellPossibleUrl> getPossibleUrls() { return possibleUrls; }
    public void setPossibleUrls(List<PermissionRequestShellPossibleUrl> possibleUrls) { this.possibleUrls = possibleUrls; }

    public Boolean getHasWriteFileRedirection() { return hasWriteFileRedirection; }
    public void setHasWriteFileRedirection(Boolean hasWriteFileRedirection) { this.hasWriteFileRedirection = hasWriteFileRedirection; }

    public Boolean getCanOfferSessionApproval() { return canOfferSessionApproval; }
    public void setCanOfferSessionApproval(Boolean canOfferSessionApproval) { this.canOfferSessionApproval = canOfferSessionApproval; }

    public String getWarning() { return warning; }
    public void setWarning(String warning) { this.warning = warning; }
}
