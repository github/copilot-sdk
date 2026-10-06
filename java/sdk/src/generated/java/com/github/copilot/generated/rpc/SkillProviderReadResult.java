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
 * Text-only SKILL.md content returned by an SDK session's skill provider. YAML frontmatter is optional: fields it omits come from the catalog descriptor, fields it declares must match the descriptor, and `allowed-tools` is read only from frontmatter. Related files and assets are not supported.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SkillProviderReadResult(
    /** SKILL.md text, with or without YAML frontmatter, or null when the provider has no skill with the requested name. The runtime enforces a 1 MiB UTF-8 byte limit. */
    @JsonProperty("markdown") String markdown
) {
}
