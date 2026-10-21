/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * The settings a consumer should act on inside a repository, together with any diagnostic raised while reading the repository layer.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsLoadAndMergeRepoResult(
    /** The merged settings document, keyed by setting name. Equals the supplied user layer when the repository declares no settings. */
    @JsonProperty("settings") Map<String, Object> settings,
    /** Why part of the repository document was ignored, as a single human-readable string joining every diagnostic with `; `. Null when it loaded cleanly. The settings that did load are still merged, so a consumer should surface this and continue rather than treat it as a failure. */
    @JsonProperty("warning") String warning
) {
}
