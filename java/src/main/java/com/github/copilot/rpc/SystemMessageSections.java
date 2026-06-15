/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

/**
 * Well-known system message section identifiers for use with
 * {@link SystemMessageMode#CUSTOMIZE} mode.
 * <p>
 * Each constant names a section of the default Copilot system message. Pass
 * these as keys in the {@code sections} map of {@link SystemMessageConfig} to
 * override individual sections.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * var config = new SystemMessageConfig().setMode(SystemMessageMode.CUSTOMIZE).setSections(Map.of(
 * 		SystemMessageSections.TONE,
 * 		new SectionOverride().setAction(SectionOverrideAction.REPLACE).setContent("Always be concise."),
 * 		SystemMessageSections.CODE_CHANGE_RULES, new SectionOverride().setAction(SectionOverrideAction.REMOVE)));
 * }</pre>
 *
 * @see SystemMessageConfig
 * @see SectionOverride
 * @since 1.0.2
 */
public sealed class SystemMessageSections permits SystemPromptSections {

    /** Agent identity preamble and mode statement. */
    public static final String IDENTITY = "identity";

    /** Response style, conciseness rules, output formatting preferences. */
    public static final String TONE = "tone";

    /** Tool usage patterns, parallel calling, batching guidelines. */
    public static final String TOOL_EFFICIENCY = "tool_efficiency";

    /** CWD, OS, git root, directory listing, available tools. */
    public static final String ENVIRONMENT_CONTEXT = "environment_context";

    /** Coding rules, linting/testing, ecosystem tools, style. */
    public static final String CODE_CHANGE_RULES = "code_change_rules";

    /** Tips, behavioral best practices, behavioral guidelines. */
    public static final String GUIDELINES = "guidelines";

    /** Environment limitations, prohibited actions, security policies. */
    public static final String SAFETY = "safety";

    /** Per-tool usage instructions. */
    public static final String TOOL_INSTRUCTIONS = "tool_instructions";

    /** Repository and organization custom instructions. */
    public static final String CUSTOM_INSTRUCTIONS = "custom_instructions";

    /**
     * Runtime-provided context and instructions (e.g. system notifications,
     * memories, workspace context, mode-specific instructions, content-exclusion
     * policy).
     *
     * @since 1.3.0
     */
    public static final String RUNTIME_INSTRUCTIONS = "runtime_instructions";

    /**
     * End-of-prompt instructions: parallel tool calling, persistence, task
     * completion.
     */
    public static final String LAST_INSTRUCTIONS = "last_instructions";

    /** Package-private constructor for the sealed hierarchy. */
    SystemMessageSections() {
    }
}
