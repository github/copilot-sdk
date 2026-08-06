/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/
package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/** Enterprise permission policy injected by an SDK host at session startup. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ManagedSettingsPermissions {
    @JsonProperty("disableBypassPermissionsMode")
    private String disableBypassPermissionsMode;
    private List<String> deny;
    private List<String> ask;
    private List<String> allow;

    /** @return the bypass permission policy, or {@code null} when unset */
    public String getDisableBypassPermissionsMode() {
        return disableBypassPermissionsMode;
    }

    /**
     * @param value
     *            must be {@code "disable"}
     * @return this policy
     */
    public ManagedSettingsPermissions setDisableBypassPermissionsMode(String value) {
        if (!"disable".equals(value)) {
            throw new IllegalArgumentException("disableBypassPermissionsMode must be \"disable\"");
        }
        this.disableBypassPermissionsMode = value;
        return this;
    }

    /** @return deny rules, or {@code null} when unset */
    public List<String> getDeny() {
        return deny;
    }

    /**
     * @param rules
     *            deny rules; @return this policy
     */
    public ManagedSettingsPermissions setDeny(List<String> rules) {
        this.deny = rules == null ? null : new ArrayList<>(rules);
        return this;
    }

    /** @return ask rules, or {@code null} when unset */
    public List<String> getAsk() {
        return ask;
    }

    /**
     * @param rules
     *            ask rules; @return this policy
     */
    public ManagedSettingsPermissions setAsk(List<String> rules) {
        this.ask = rules == null ? null : new ArrayList<>(rules);
        return this;
    }

    /** @return allow rules, or {@code null} when unset */
    public List<String> getAllow() {
        return allow;
    }

    /**
     * @param rules
     *            allow rules; @return this policy
     */
    public ManagedSettingsPermissions setAllow(List<String> rules) {
        this.allow = rules == null ? null : new ArrayList<>(rules);
        return this;
    }
}
