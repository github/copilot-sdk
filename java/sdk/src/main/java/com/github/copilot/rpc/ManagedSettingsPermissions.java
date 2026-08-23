/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/
package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.generated.rpc.DisableBypassPermissionsMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise permission policy injected by an SDK host at session startup.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ManagedSettingsPermissions {
    @JsonProperty("disableBypassPermissionsMode")
    private String disableBypassPermissionsMode;

    @JsonProperty("deny")
    private List<String> deny;

    @JsonProperty("ask")
    private List<String> ask;

    @JsonProperty("allow")
    private List<String> allow;

    /** @return the bypass-permissions policy, or {@code null} when unset */
    public String getDisableBypassPermissionsMode() {
        return disableBypassPermissionsMode;
    }

    /**
     * @param value
     *            bypass-permissions policy
     * @return this policy
     */
    public ManagedSettingsPermissions setDisableBypassPermissionsMode(String value) {
        this.disableBypassPermissionsMode = value;
        return this;
    }

    /**
     * Sets the bypass-permissions policy from the generated enum retained for
     * source compatibility.
     *
     * @param value
     *            bypass-permissions policy
     * @return this policy
     */
    public ManagedSettingsPermissions setDisableBypassPermissionsMode(DisableBypassPermissionsMode value) {
        this.disableBypassPermissionsMode = value == null ? null : value.getValue();
        return this;
    }

    /** @return rules that deny matching operations, or {@code null} when unset */
    public List<String> getDeny() {
        return deny;
    }

    /**
     * @param rules
     *            deny rules
     * @return this policy
     */
    public ManagedSettingsPermissions setDeny(List<String> rules) {
        this.deny = rules == null ? null : new ArrayList<>(rules);
        return this;
    }

    /** @return rules that require approval, or {@code null} when unset */
    public List<String> getAsk() {
        return ask;
    }

    /**
     * @param rules
     *            ask rules
     * @return this policy
     */
    public ManagedSettingsPermissions setAsk(List<String> rules) {
        this.ask = rules == null ? null : new ArrayList<>(rules);
        return this;
    }

    /** @return rules that allow matching operations, or {@code null} when unset */
    public List<String> getAllow() {
        return allow;
    }

    /**
     * @param rules
     *            allow rules
     * @return this policy
     */
    public ManagedSettingsPermissions setAllow(List<String> rules) {
        this.allow = rules == null ? null : new ArrayList<>(rules);
        return this;
    }
}
