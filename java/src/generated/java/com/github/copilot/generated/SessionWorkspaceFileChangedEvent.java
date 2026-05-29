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
 * Session event "session.workspace_file_changed". Workspace file change details including path and operation type
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionWorkspaceFileChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.workspace_file_changed"; }

    @JsonProperty("data")
    private SessionWorkspaceFileChangedEventData data;

    public SessionWorkspaceFileChangedEventData getData() { return data; }
    public void setData(SessionWorkspaceFileChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionWorkspaceFileChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionWorkspaceFileChangedEventData(
        /** Relative path within the session workspace files directory */
        @JsonProperty("path") String path,
        /** Whether the file was newly created or updated */
        @JsonProperty("operation") WorkspaceFileChangedOperation operation
    ) {
    }
}
