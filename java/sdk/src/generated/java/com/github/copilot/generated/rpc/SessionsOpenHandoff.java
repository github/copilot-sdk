/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Parameters for fetching a remote session and handing it off to a new local session.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionsOpenHandoff extends SessionsOpenParams {

    @JsonProperty("kind")
    private final String kind = "handoff";

    @Override
    public String getKind() { return kind; }

    /** Remote session metadata for the session to hand off (typically obtained from `sessions.list` with `source: "remote"`). */
    @JsonProperty("metadata")
    private RemoteSessionMetadataValue metadata;

    /** Session construction options for the new local session. */
    @JsonProperty("options")
    private SessionOpenOptions options;

    /** Task type determines the handoff strategy (CCA fetches events; CLI prepares a transient session). */
    @JsonProperty("taskType")
    private SessionsOpenHandoffTaskType taskType;

    /** In-process progress callback `(update) => void` invoked for each handoff step. Marked internal because a function reference cannot cross the JSON-RPC boundary. The host-side `handoffSession` is already declared as `AsyncGenerator<HandoffProgress, HandoffResult>`; the schema layer flattens it because it does not yet support streaming methods. The wire-clean replacement is to expose the AsyncGenerator directly (or use vscode-jsonrpc `$/progress` notifications) once the schema/transport layer supports it. */
    @JsonProperty("onProgress")
    private Object onProgress;

    /** In-process confirmation callback `(request) => boolean | Promise<boolean>` invoked when the handoff needs the caller to confirm a non-fatal blocker (e.g. a repository mismatch between the current working directory and the remote session). Returning `true` proceeds with the handoff; returning `false` (or omitting the callback) aborts it. Marked internal because a function reference cannot cross the JSON-RPC boundary, for the same reasons as `onProgress`. */
    @JsonProperty("onConfirm")
    private Object onConfirm;

    public RemoteSessionMetadataValue getMetadata() { return metadata; }
    public void setMetadata(RemoteSessionMetadataValue metadata) { this.metadata = metadata; }

    public SessionOpenOptions getOptions() { return options; }
    public void setOptions(SessionOpenOptions options) { this.options = options; }

    public SessionsOpenHandoffTaskType getTaskType() { return taskType; }
    public void setTaskType(SessionsOpenHandoffTaskType taskType) { this.taskType = taskType; }

    public Object getOnProgress() { return onProgress; }
    public void setOnProgress(Object onProgress) { this.onProgress = onProgress; }

    public Object getOnConfirm() { return onConfirm; }
    public void setOnConfirm(Object onConfirm) { this.onConfirm = onConfirm; }
}
