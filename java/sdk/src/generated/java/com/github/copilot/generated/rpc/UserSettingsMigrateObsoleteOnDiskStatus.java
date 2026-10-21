/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * What the one-time obsolete-settings cleanup did to the document.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
enum UserSettingsMigrateObsoleteOnDiskStatus {
    /** The {@code missing} variant. */
    MISSING("missing"),
    /** The {@code invalid} variant. */
    INVALID("invalid"),
    /** The {@code unchanged} variant. */
    UNCHANGED("unchanged"),
    /** The {@code changed} variant. */
    CHANGED("changed");

    private final String value;
    UserSettingsMigrateObsoleteOnDiskStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static UserSettingsMigrateObsoleteOnDiskStatus fromValue(String value) {
        for (UserSettingsMigrateObsoleteOnDiskStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown UserSettingsMigrateObsoleteOnDiskStatus value: " + value);
    }
}
