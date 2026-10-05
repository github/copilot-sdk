/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Component of session customization discovery.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CustomizationReloadSubsystem {
    /** The {@code repositoryContext} variant. */
    public static final CustomizationReloadSubsystem REPOSITORYCONTEXT = new CustomizationReloadSubsystem("repositoryContext");
    /** The {@code instructions} variant. */
    public static final CustomizationReloadSubsystem INSTRUCTIONS = new CustomizationReloadSubsystem("instructions");
    /** The {@code plugins} variant. */
    public static final CustomizationReloadSubsystem PLUGINS = new CustomizationReloadSubsystem("plugins");
    /** The {@code hooks} variant. */
    public static final CustomizationReloadSubsystem HOOKS = new CustomizationReloadSubsystem("hooks");
    /** The {@code skills} variant. */
    public static final CustomizationReloadSubsystem SKILLS = new CustomizationReloadSubsystem("skills");
    /** The {@code agents} variant. */
    public static final CustomizationReloadSubsystem AGENTS = new CustomizationReloadSubsystem("agents");
    /** The {@code mcp} variant. */
    public static final CustomizationReloadSubsystem MCP = new CustomizationReloadSubsystem("mcp");
    /** The {@code extensions} variant. */
    public static final CustomizationReloadSubsystem EXTENSIONS = new CustomizationReloadSubsystem("extensions");
    /** The default value when no recognized variant is available. */
    public static final CustomizationReloadSubsystem UNKNOWN = new CustomizationReloadSubsystem("unknown");

    private final String value;
    private CustomizationReloadSubsystem(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CustomizationReloadSubsystem fromValue(String value) {
        if (REPOSITORYCONTEXT.value.equals(value)) return REPOSITORYCONTEXT;
        if (INSTRUCTIONS.value.equals(value)) return INSTRUCTIONS;
        if (PLUGINS.value.equals(value)) return PLUGINS;
        if (HOOKS.value.equals(value)) return HOOKS;
        if (SKILLS.value.equals(value)) return SKILLS;
        if (AGENTS.value.equals(value)) return AGENTS;
        if (MCP.value.equals(value)) return MCP;
        if (EXTENSIONS.value.equals(value)) return EXTENSIONS;
        if (UNKNOWN.value.equals(value)) return UNKNOWN;
        if (value != null) return new CustomizationReloadSubsystem(value);
        throw new IllegalArgumentException("Unknown CustomizationReloadSubsystem value: " + value);
    }
    @Override
    public boolean equals(Object other) {
        return other instanceof CustomizationReloadSubsystem that && value.equals(that.value);
    }
    @Override
    public int hashCode() { return value.hashCode(); }
}
