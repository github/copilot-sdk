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
 * Catalog-only metadata for one SDK-provided skill. The complete SKILL.md is fetched separately and lazily.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillProviderDescriptor(
    /** Invocation and display name. */
    @JsonProperty("name") String name,
    /** Description used in skill catalogs without fetching content. */
    @JsonProperty("description") String description,
    /** Whether users may invoke the skill directly. Defaults to true. */
    @JsonProperty("userInvocable") Boolean userInvocable,
    /** Whether model invocation is disabled. Defaults to false. */
    @JsonProperty("disableModelInvocation") Boolean disableModelInvocation,
    /** Optional freeform argument hint used by slash-command catalogs. */
    @JsonProperty("argumentHint") String argumentHint
) {
}
