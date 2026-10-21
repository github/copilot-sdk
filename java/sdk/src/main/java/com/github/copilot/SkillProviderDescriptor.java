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
 * @param directory
 *            the absolute path to the folder on local disk that holds the
 *            skill's supporting files (scripts, references, templates, assets),
 *            or {@code null} when the skill has none. When the skill is
 *            activated, the runtime lists the folder's files for the model, as
 *            it does for a file-based skill's folder; it never reads or copies
 *            them. The folder becomes a read-only skill root, like a configured
 *            skill directory. It is not shown in skill catalogs.
 * @apiNote This API is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SkillProviderDescriptor(@JsonProperty("name") String name,
        @JsonProperty("description") String description, @JsonProperty("userInvocable") Boolean userInvocable,
        @JsonProperty("disableModelInvocation") Boolean disableModelInvocation,
        @JsonProperty("argumentHint") String argumentHint, @JsonProperty("directory") String directory) {

    /**
     * Creates a descriptor for a skill without supporting files.
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
     */
    public SkillProviderDescriptor(String name, String description, Boolean userInvocable,
            Boolean disableModelInvocation, String argumentHint) {
        this(name, description, userInvocable, disableModelInvocation, argumentHint, null);
    }
}
