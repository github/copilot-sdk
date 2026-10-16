/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Catalog metadata for one skill supplied by a {@link SkillProvider}.
 * <p>
 * The runtime lists these descriptors without fetching skill content, then
 * calls {@link SkillProvider#readSkill(String)} when the skill is used.
 *
 * @param name
 *            the invocation and display name
 * @param description
 *            the description shown in skill catalogs
 * @param userInvocable
 *            whether users may invoke the skill directly, or {@code null} for
 *            the default ({@code true})
 * @param disableModelInvocation
 *            whether model invocation is disabled, or {@code null} for the
 *            default ({@code false})
 * @param argumentHint
 *            an optional freeform argument hint for slash-command catalogs
 * @apiNote This API is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SkillProviderDescriptor(@JsonProperty("name") String name,
        @JsonProperty("description") String description, @JsonProperty("userInvocable") Boolean userInvocable,
        @JsonProperty("disableModelInvocation") Boolean disableModelInvocation,
        @JsonProperty("argumentHint") String argumentHint) {
}
