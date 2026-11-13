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
 * Complete text used to generate one session diff. These are display contents, with the same text decoding as the patch, not a file-restore contract.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkspaceDiffContents(
    /** Complete text before the session first changed the file. Omitted when the file did not exist; an empty string represents an existing empty file. */
    @JsonProperty("before") String before,
    /** Complete current text read when computing the diff. Omitted for a deleted file; an empty string represents an existing empty file. */
    @JsonProperty("after") String after
) {
}
