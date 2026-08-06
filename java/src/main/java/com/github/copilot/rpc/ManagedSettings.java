/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/
package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Permissions-only managed settings injected by an SDK host. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ManagedSettings {
    private ManagedSettingsPermissions permissions;

    /** @return the managed permission policy, or {@code null} when unset */
    public ManagedSettingsPermissions getPermissions() {
        return permissions;
    }

    /**
     * @param permissions
     *            managed permission policy
     * @return this settings object
     */
    public ManagedSettings setPermissions(ManagedSettingsPermissions permissions) {
        this.permissions = permissions;
        return this;
    }
}
