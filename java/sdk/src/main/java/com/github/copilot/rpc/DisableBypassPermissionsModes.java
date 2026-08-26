/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/
package com.github.copilot.rpc;

/**
 * Known values for the managed bypass-permissions policy.
 *
 * <p>
 * The wire contract is an open string so callers can pass newer fail-closed
 * modes directly to
 * {@link ManagedSettingsPermissions#setDisableBypassPermissionsMode(String)}.
 */
public final class DisableBypassPermissionsModes {
    /** Turns off bypass-permissions mode. */
    public static final String DISABLE = "disable";

    /** Permits bypass only for automatic operations. */
    public static final String ALLOW_AUTO_ONLY = "allow-auto-only";

    private DisableBypassPermissionsModes() {
    }
}
