/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Describes the outcome kind of a permission request result.
 *
 * <p>
 * This is a string-backed value type that can hold both well-known kinds (via
 * the static constants) and arbitrary extension values forwarded by the server.
 * Comparisons are case-insensitive to match server behaviour.
 *
 * <h2>Well-known kinds</h2>
 * <ul>
 * <li>{@link #APPROVED} — the permission was approved for this one
 * instance.</li>
 * <li>{@link #REJECTED} — the permission was denied interactively by the
 * user.</li>
 * <li>{@link #USER_NOT_AVAILABLE} — the permission was denied because user
 * confirmation was unavailable.</li>
 * <li>{@link #NO_RESULT} — no permission decision was made.</li>
 * </ul>
 *
 * @see PermissionRequestResult
 * @since 1.1.0
 */
public final class PermissionRequestResultKind {

    /** The permission was approved for this one instance. */
    public static final PermissionRequestResultKind APPROVED = new PermissionRequestResultKind("approve-once");

    /** The permission was denied interactively by the user. */
    public static final PermissionRequestResultKind REJECTED = new PermissionRequestResultKind("reject");

    /** The permission was denied because user confirmation was unavailable. */
    public static final PermissionRequestResultKind USER_NOT_AVAILABLE = new PermissionRequestResultKind(
            "user-not-available");

    /**
     * Leaves the pending permission request unanswered.
     * <p>
     * When the SDK is used as an extension and the extension's permission handler
     * cannot or chooses not to handle a given permission request, it can return
     * {@code NO_RESULT} to leave the request unanswered, allowing another client to
     * handle it.
     * <p>
     * <strong>Warning:</strong> This kind is only valid with protocol v3 servers
     * (broadcast permission model). When connected to a protocol v2 server, the SDK
     * will throw {@link IllegalStateException} because v2 expects exactly one
     * response per permission request.
     */
    public static final PermissionRequestResultKind NO_RESULT = new PermissionRequestResultKind("no-result");

    /**
     * @deprecated Use {@link #REJECTED} instead.
     */
    @Deprecated
    public static final PermissionRequestResultKind DENIED_INTERACTIVELY_BY_USER = REJECTED;

    /**
     * @deprecated Use {@link #USER_NOT_AVAILABLE} instead.
     */
    @Deprecated
    public static final PermissionRequestResultKind DENIED_COULD_NOT_REQUEST_FROM_USER = USER_NOT_AVAILABLE;

    /**
     * @deprecated Use {@link #USER_NOT_AVAILABLE} instead.
     */
    @Deprecated
    public static final PermissionRequestResultKind DENIED_BY_RULES = USER_NOT_AVAILABLE;

    private final String value;

    /**
     * Creates a new {@code PermissionRequestResultKind} with the given string
     * value. Useful for extension kinds not covered by the well-known constants.
     *
     * @param value
     *            the string value; {@code null} is treated as an empty string
     */
    @JsonCreator
    public PermissionRequestResultKind(String value) {
        this.value = value != null ? value : "";
    }

    /**
     * Returns the underlying string value of this kind.
     *
     * @return the string value, never {@code null}
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PermissionRequestResultKind)) {
            return false;
        }
        PermissionRequestResultKind other = (PermissionRequestResultKind) obj;
        return value.equalsIgnoreCase(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value.toLowerCase(java.util.Locale.ROOT));
    }
}
