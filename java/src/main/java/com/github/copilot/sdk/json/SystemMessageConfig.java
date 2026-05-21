/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.copilot.sdk.SystemMessageMode;

/**
 * Configuration for customizing the system message.
 * <p>
 * The system message controls the behavior and personality of the AI assistant.
 * This configuration allows you to either append to, replace, or fine-tune the
 * default system message.
 *
 * <h2>Example - Append Mode</h2>
 *
 * <pre>{@code
 * var config = new SystemMessageConfig().setMode(SystemMessageMode.APPEND)
 * 		.setContent("Always respond in a formal tone.");
 * }</pre>
 *
 * <h2>Example - Replace Mode</h2>
 *
 * <pre>{@code
 * var config = new SystemMessageConfig().setMode(SystemMessageMode.REPLACE)
 * 		.setContent("You are a helpful coding assistant.");
 * }</pre>
 *
 * <h2>Example - Customize Mode</h2>
 *
 * <pre>{@code
 * var config = new SystemMessageConfig().setMode(SystemMessageMode.CUSTOMIZE)
 * 		.setSections(
 * 				Map.of(SystemPromptSections.TONE,
 * 						new SectionOverride().setAction(SectionOverrideAction.REPLACE)
 * 								.setContent("Be concise and formal."),
 * 						SystemPromptSections.CODE_CHANGE_RULES,
 * 						new SectionOverride().setAction(SectionOverrideAction.REMOVE)))
 * 		.setContent("Additional instructions appended after all sections.");
 * }</pre>
 *
 * @see SessionConfig#setSystemMessage(SystemMessageConfig)
 * @see SystemMessageMode
 * @see SectionOverride
 * @see SystemPromptSections
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemMessageConfig {

    private SystemMessageMode mode;
    private String content;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.annotation.JsonProperty("sections")
    private Map<String, SectionOverride> sections;

    /**
     * Gets the system message mode.
     *
     * @return the mode (APPEND, REPLACE, or CUSTOMIZE)
     */
    public SystemMessageMode getMode() {
        return mode;
    }

    /**
     * Sets the system message mode.
     * <p>
     * Use {@link SystemMessageMode#APPEND} to add to the default system message
     * while preserving guardrails, {@link SystemMessageMode#REPLACE} to fully
     * customize the system message, or {@link SystemMessageMode#CUSTOMIZE} to
     * override individual sections.
     *
     * @param mode
     *            the mode (APPEND, REPLACE, or CUSTOMIZE)
     * @return this config for method chaining
     */
    public SystemMessageConfig setMode(SystemMessageMode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Gets the system message content.
     *
     * @return the content to append or use as replacement
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the system message content.
     * <p>
     * For {@link SystemMessageMode#APPEND} and {@link SystemMessageMode#REPLACE}
     * modes, this is the primary content. For {@link SystemMessageMode#CUSTOMIZE}
     * mode, this is appended after all section overrides.
     *
     * @param content
     *            the system message content
     * @return this config for method chaining
     */
    public SystemMessageConfig setContent(String content) {
        this.content = content;
        return this;
    }

    /**
     * Gets the section-level overrides for {@link SystemMessageMode#CUSTOMIZE}
     * mode.
     *
     * @return the sections map, or {@code null}
     */
    public Map<String, SectionOverride> getSections() {
        return sections;
    }

    /**
     * Sets section-level overrides for {@link SystemMessageMode#CUSTOMIZE} mode.
     * <p>
     * Keys are section identifiers from {@link SystemPromptSections}. Each value
     * describes how that section should be modified. Sections with a
     * {@link SectionOverride#getTransform() transform} callback are handled locally
     * by the SDK via a {@code systemMessage.transform} RPC call; the rest are sent
     * to the CLI as-is.
     *
     * @param sections
     *            a map of section identifier to override operation
     * @return this config for method chaining
     * @since 1.2.0
     */
    public SystemMessageConfig setSections(Map<String, SectionOverride> sections) {
        this.sections = sections;
        return this;
    }
}
