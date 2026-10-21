/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/
package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise permission policy injected by an SDK host at session startup.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ManagedSettingsPermissions {
    @JsonProperty("disableBypassPermissionsMode")
    private String disableBypassPermissionsMode;

    @JsonProperty("disableAssistedPermissionsMode")
    private Boolean disableAssistedPermissionsMode;

    @JsonProperty("deny")
    private List<String> deny;

    @JsonProperty("ask")
    private List<String> ask;

    @JsonProperty("allow")
    private List<String> allow;

    @JsonProperty("limitTo")
    private List<String> limitTo;

    /** @return the bypass-permissions policy, or {@code null} when unset */
    public String getDisableBypassPermissionsMode() {
        return disableBypassPermissionsMode;
    }

    /**
     * Restricts bypass/allow-all permission modes. See
     * {@link DisableBypassPermissionsModes} for known values. Newer values are
     * forwarded unchanged so runtime policies remain fail-closed.
     *
     * @param value
     *            bypass-permissions policy
     * @return this policy
     */
    public ManagedSettingsPermissions setDisableBypassPermissionsMode(String value) {
        this.disableBypassPermissionsMode = value;
        return this;
    }

    /**
     * @return whether Assisted Permissions are disabled, or {@code null} when unset
     */
    public Boolean getDisableAssistedPermissionsMode() {
        return disableAssistedPermissionsMode;
    }

    /**
     * Prevents Assisted Permissions from being activated when true.
     *
     * @param value
     *            whether Assisted Permissions are disabled
     * @return this policy
     */
    public ManagedSettingsPermissions setDisableAssistedPermissionsMode(Boolean value) {
        this.disableAssistedPermissionsMode = value;
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

    /**
     * @return the closed-world host boundary, or {@code null} when unset
     */
    public List<String> getLimitTo() {
        return limitTo;
    }

    /**
     * Every network request must match this list, and multiple managed layers
     * intersect their lists. A present empty list denies all hosts.
     *
     * @param rules
     *            host-only {@code Domain(hostname)}, {@code Domain(IP)}, or
     *            {@code Domain(*.example.com)} boundary rules
     * @return this policy
     */
    public ManagedSettingsPermissions setLimitTo(List<String> rules) {
        this.limitTo = rules == null ? null : new ArrayList<>(rules);
        return this;
    }
}
