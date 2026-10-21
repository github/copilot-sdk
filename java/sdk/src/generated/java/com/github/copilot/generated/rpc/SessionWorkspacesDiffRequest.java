/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.Objects;
import javax.annotation.processing.Generated;

/**
 * Parameters for computing a workspace diff.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionWorkspacesDiffRequest {

    /** Diff mode requested by the client. */
    @JsonProperty("mode")
    private final WorkspaceDiffMode mode;

    /** When true, ignore whitespace-only changes (git `--ignore-all-space`). Defaults to false. */
    @JsonProperty("ignoreWhitespace")
    private Boolean ignoreWhitespace;

    /** Include the full before/after text used to compute each session diff. Defaults to false; true is accepted only for session mode. Existing capture/read limits still apply, and binary or unavailable contents are not returned. This can substantially increase response size. */
    @JsonProperty("includeContents")
    private Boolean includeContents;

    /**
     * Creates a request with its required inputs.
     *
     * @param mode Diff mode requested by the client.
     */
    public SessionWorkspacesDiffRequest(WorkspaceDiffMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    /**
     * Returns the {@code mode} property.
     *
     * @return Diff mode requested by the client.
     */
    public WorkspaceDiffMode getMode() {
        return mode;
    }

    /**
     * Returns the {@code ignoreWhitespace} property.
     *
     * @return When true, ignore whitespace-only changes (git `--ignore-all-space`). Defaults to false.
     */
    public Boolean getIgnoreWhitespace() {
        return ignoreWhitespace;
    }

    /**
     * Returns the {@code includeContents} property.
     *
     * @return Include the full before/after text used to compute each session diff. Defaults to false; true is accepted only for session mode. Existing capture/read limits still apply, and binary or unavailable contents are not returned. This can substantially increase response size.
     */
    public Boolean getIncludeContents() {
        return includeContents;
    }

    /**
     * Sets the {@code ignoreWhitespace} property.
     *
     * @param value When true, ignore whitespace-only changes (git `--ignore-all-space`). Defaults to false.
     * @return this request
     */
    public SessionWorkspacesDiffRequest setIgnoreWhitespace(Boolean value) {
        this.ignoreWhitespace = value;
        return this;
    }

    /**
     * Sets the {@code includeContents} property.
     *
     * @param value Include the full before/after text used to compute each session diff. Defaults to false; true is accepted only for session mode. Existing capture/read limits still apply, and binary or unavailable contents are not returned. This can substantially increase response size.
     * @return this request
     */
    public SessionWorkspacesDiffRequest setIncludeContents(Boolean value) {
        this.includeContents = value;
        return this;
    }
}
