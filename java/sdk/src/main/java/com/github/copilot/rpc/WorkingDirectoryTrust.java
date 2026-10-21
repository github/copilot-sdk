/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Scope of a working-directory trust grant made during session creation.
 */
public enum WorkingDirectoryTrust {
    /** Trust the directory for this session only. */
    SESSION("session"),
    /** Persist trust in the user's profile. */
    USER("user");

    private final String value;

    WorkingDirectoryTrust(String value) {
        this.value = value;
    }

    /**
     * Returns the protocol value of this trust scope.
     *
     * @return {@code "session"} or {@code "user"}
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Parses a protocol trust scope.
     *
     * @param value
     *            the protocol value
     * @return the corresponding trust scope
     * @throws IllegalArgumentException
     *             if the value is not a supported scope
     */
    @JsonCreator
    public static WorkingDirectoryTrust fromValue(String value) {
        for (var scope : values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unsupported working-directory trust scope: " + value);
    }
}
