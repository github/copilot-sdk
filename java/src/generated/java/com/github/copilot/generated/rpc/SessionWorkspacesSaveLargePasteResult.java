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
 * Descriptor for the saved paste file, or null when the workspace is unavailable.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkspacesSaveLargePasteResult(
    /** Saved-paste descriptor, or null when the workspace is unavailable (e.g. CCA runtime, non-infinite sessions, remote sessions) */
    @JsonProperty("saved") SessionWorkspacesSaveLargePasteResultSaved saved
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionWorkspacesSaveLargePasteResultSaved(
        /** Absolute filesystem path to the saved paste file */
        @JsonProperty("filePath") String filePath,
        /** Filename within the workspace files directory */
        @JsonProperty("filename") String filename,
        /** Size of the saved file in bytes */
        @JsonProperty("sizeBytes") Long sizeBytes
    ) {
    }
}
