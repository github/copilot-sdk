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
 * One reviewed Skill file.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillInstallationFileReview(
    /** Relative file path within the Skill root. */
    @JsonProperty("path") String path,
    /** Exact reviewed file size in bytes. */
    @JsonProperty("sizeBytes") Long sizeBytes,
    /** Declared media type for the file. */
    @JsonProperty("mediaType") String mediaType,
    /** Whether the file is installed with executable permissions. */
    @JsonProperty("executable") Boolean executable,
    /** SHA-256 digest of the exact file bytes. */
    @JsonProperty("digest") String digest
) {
}
