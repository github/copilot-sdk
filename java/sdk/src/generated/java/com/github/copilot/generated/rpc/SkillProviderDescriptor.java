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
 * Authoritative catalog metadata for one SDK-provided skill. The skill's SKILL.md text is fetched separately and lazily.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SkillProviderDescriptor(
    /** Invocation and display name. */
    @JsonProperty("name") String name,
    /** Description used in skill catalogs without fetching content. */
    @JsonProperty("description") String description,
    /** Whether users may invoke the skill directly. Defaults to true. */
    @JsonProperty("userInvocable") Boolean userInvocable,
    /** Whether model invocation is disabled. Defaults to false. */
    @JsonProperty("disableModelInvocation") Boolean disableModelInvocation,
    /** Optional freeform argument hint used by slash-command catalogs. */
    @JsonProperty("argumentHint") String argumentHint,
    /** Absolute path to the folder that holds the skill's supporting files (scripts, references, templates, assets), in the session's path conventions: the host OS for local disk, or the registered SessionFs conventions. When the skill is activated the runtime lists the folder's files for the model through the session's filesystem, with the same rules as a file-based skill's folder; it never reads or copies them. On local disk the folder becomes a read-only skill root, like a configured skill directory. It is not shown in skill catalogs. */
    @JsonProperty("directory") String directory
) {
}
