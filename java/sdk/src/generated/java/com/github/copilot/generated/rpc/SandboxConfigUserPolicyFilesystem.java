/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Filesystem rules to merge into the base policy.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandboxConfigUserPolicyFilesystem(
    /** Paths granted read/write access. */
    @JsonProperty("readwritePaths") List<String> readwritePaths,
    /** Paths granted read-only access. */
    @JsonProperty("readonlyPaths") List<String> readonlyPaths,
    /** Paths explicitly denied. */
    @JsonProperty("deniedPaths") List<String> deniedPaths,
    /** Paths granted enumerate-only access: sandboxed commands can list these directories without reading the files in them. Honored only on Windows hosts with the `filesystem_enumeration` capability; ignored elsewhere. An entry at or under a read-only, read-write, or denied path is ignored, so it never narrows a grant or reopens a denial. */
    @JsonProperty("enumeratePaths") List<String> enumeratePaths
) {

    /**
     * Creates filesystem rules without enumerate-only paths.
     *
     * @param readwritePaths Paths granted read/write access.
     * @param readonlyPaths Paths granted read-only access.
     * @param deniedPaths Paths explicitly denied.
     */
    public SandboxConfigUserPolicyFilesystem(
        List<String> readwritePaths,
        List<String> readonlyPaths,
        List<String> deniedPaths
    ) {
        this(readwritePaths, readonlyPaths, deniedPaths, null);
    }
}
