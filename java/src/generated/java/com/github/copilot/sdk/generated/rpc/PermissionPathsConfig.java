/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionPathsConfig(
    /** If true, the runtime allows access to all paths without prompting. Equivalent to constructing an UnrestrictedPathManager. */
    @JsonProperty("unrestricted") Boolean unrestricted,
    /** Additional directories to allow tool access to (in addition to the session's working directory). When `unrestricted` is true, these are still pre-populated on the UnrestrictedPathManager so they remain visible via getDirectories() (e.g. for @-mention completion). */
    @JsonProperty("additionalDirectories") List<String> additionalDirectories,
    /** Whether to include the system temp directory in the allowed list (defaults to true). Ignored when `unrestricted` is true. */
    @JsonProperty("includeTempDirectory") Boolean includeTempDirectory,
    /** Workspace root path (special-cased to be allowed even before the directory exists). Ignored when `unrestricted` is true. */
    @JsonProperty("workspacePath") String workspacePath
) {
}
