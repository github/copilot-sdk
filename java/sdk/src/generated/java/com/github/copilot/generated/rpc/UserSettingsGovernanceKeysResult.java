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
 * Top-level user-setting keys grouped by who may govern them.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsGovernanceKeysResult(
    /** Top-level keys a repository may override through `.github/copilot/settings.json` or `.github/copilot/settings.local.json`. */
    @JsonProperty("repo") List<String> repo,
    /** Top-level keys a repository may override through a Claude-compatible repository config. */
    @JsonProperty("claudeRepo") List<String> claudeRepo,
    /** Top-level keys an enterprise policy may fix through managed settings. */
    @JsonProperty("managed") List<String> managed
) {
}
