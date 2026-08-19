/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Origin of an effective session model change.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum ModelChangeSource {
    /** The {@code model_command} variant. */
    MODEL_COMMAND("model_command"),
    /** The {@code settings_command} variant. */
    SETTINGS_COMMAND("settings_command"),
    /** The {@code config_command} variant. */
    CONFIG_COMMAND("config_command"),
    /** The {@code model_picker} variant. */
    MODEL_PICKER("model_picker"),
    /** The {@code managed_settings} variant. */
    MANAGED_SETTINGS("managed_settings"),
    /** The {@code repo_settings} variant. */
    REPO_SETTINGS("repo_settings"),
    /** The {@code startup} variant. */
    STARTUP("startup"),
    /** The {@code agent} variant. */
    AGENT("agent"),
    /** The {@code plan_mode} variant. */
    PLAN_MODE("plan_mode"),
    /** The {@code automatic} variant. */
    AUTOMATIC("automatic"),
    /** The {@code sdk} variant. */
    SDK("sdk");

    private final String value;
    ModelChangeSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ModelChangeSource fromValue(String value) {
        for (ModelChangeSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown ModelChangeSource value: " + value);
    }
}
