/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/
package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Managed settings an SDK host may inject at session create or resume.
 *
 * <p>
 * The initial public contract is permissions-only.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ManagedSettings {
    @JsonProperty("permissions")
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
