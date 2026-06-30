/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.github.copilot.CopilotExperimental;

/**
 * Client environment metadata describing the process that produced a telemetry
 * event.
 *
 * <p>
 * Internal/experimental: this type is part of the GitHub telemetry forwarding
 * surface and may change or be removed without notice.
 *
 * @since 1.0.0
 */
@CopilotExperimental
public class GitHubTelemetryClientInfo {

    @JsonProperty("cli_version")
    private String cliVersion = "";

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("client_type")
    private String clientType;

    @JsonProperty("copilot_plan")
    private String copilotPlan;

    @JsonProperty("dev_device_id")
    private String devDeviceId;

    @JsonProperty("is_staff")
    private Boolean isStaff;

    @JsonProperty("node_version")
    private String nodeVersion = "";

    @JsonProperty("os_arch")
    private String osArch = "";

    @JsonProperty("os_platform")
    private String osPlatform = "";

    @JsonProperty("os_version")
    private String osVersion = "";

    /**
     * Gets the Copilot CLI version string.
     *
     * @return the CLI version
     */
    public String getCliVersion() {
        return cliVersion;
    }

    /**
     * Gets the name of the client application.
     *
     * @return the client name, or {@code null} if unknown
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * Gets the type of client.
     *
     * @return the client type, or {@code null} if unknown
     */
    public String getClientType() {
        return clientType;
    }

    /**
     * Gets the Copilot subscription plan, when known.
     *
     * @return the Copilot plan, or {@code null} if unknown
     */
    public String getCopilotPlan() {
        return copilotPlan;
    }

    /**
     * Gets the stable machine identifier for the device.
     *
     * @return the device identifier, or {@code null} if unknown
     */
    public String getDevDeviceId() {
        return devDeviceId;
    }

    /**
     * Gets whether the user is a GitHub/Microsoft staff member.
     *
     * @return the staff flag, or {@code null} if unknown
     */
    public Boolean getIsStaff() {
        return isStaff;
    }

    /**
     * Gets the Node.js runtime version string.
     *
     * @return the Node.js version
     */
    public String getNodeVersion() {
        return nodeVersion;
    }

    /**
     * Gets the operating system architecture (e.g. arm64, x64).
     *
     * @return the OS architecture
     */
    public String getOsArch() {
        return osArch;
    }

    /**
     * Gets the operating system platform (e.g. darwin, linux, win32).
     *
     * @return the OS platform
     */
    public String getOsPlatform() {
        return osPlatform;
    }

    /**
     * Gets the operating system version string.
     *
     * @return the OS version
     */
    public String getOsVersion() {
        return osVersion;
    }
}
