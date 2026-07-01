/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Client environment metadata describing the process that produced a telemetry event.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubTelemetryClientInfo(
    /** Copilot CLI version string. */
    @JsonProperty("cli_version") String cliVersion,
    /** Operating system platform (e.g. darwin, linux, win32). */
    @JsonProperty("os_platform") String osPlatform,
    /** Operating system version string. */
    @JsonProperty("os_version") String osVersion,
    /** Operating system architecture (e.g. arm64, x64). */
    @JsonProperty("os_arch") String osArch,
    /** Node.js runtime version string. */
    @JsonProperty("node_version") String nodeVersion,
    /** Copilot subscription plan, when known. */
    @JsonProperty("copilot_plan") String copilotPlan,
    /** Type of client. */
    @JsonProperty("client_type") String clientType,
    /** Name of the client application. */
    @JsonProperty("client_name") String clientName,
    /** Whether the user is a GitHub/Microsoft staff member. */
    @JsonProperty("is_staff") Boolean isStaff,
    /** Stable machine identifier for the device. */
    @JsonProperty("dev_device_id") String devDeviceId
) {
}
