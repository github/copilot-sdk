/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Owned Skill state observed from files and receipts.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SkillInstallationOwnershipState {
    /** The {@code intact} variant. */
    INTACT("intact"),
    /** The {@code modified} variant. */
    MODIFIED("modified"),
    /** The {@code recovery-required} variant. */
    RECOVERY_REQUIRED("recovery-required");

    private final String value;
    SkillInstallationOwnershipState(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SkillInstallationOwnershipState fromValue(String value) {
        for (SkillInstallationOwnershipState v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SkillInstallationOwnershipState value: " + value);
    }
}
