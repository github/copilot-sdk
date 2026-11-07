/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Client environment metadata describing the process that produced a telemetry event.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
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
    @JsonProperty("dev_device_id") String devDeviceId,
    /** Distinct CPU model names for the host, comma-separated. */
    @JsonProperty("cpu_model") String cpuModel,
    /** Number of logical CPU cores on the host. */
    @JsonProperty("cpu_count") Long cpuCount
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public GitHubTelemetryClientInfo {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String cliVersion;
        private String osPlatform;
        private String osVersion;
        private String osArch;
        private String nodeVersion;
        private String copilotPlan;
        private String clientType;
        private String clientName;
        private Boolean isStaff;
        private String devDeviceId;
        private String cpuModel;
        private Long cpuCount;

        /**
         * Sets the {@code cli_version} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder cliVersion(String value) {
            this.cliVersion = value;
            return this;
        }

        /**
         * Sets the {@code os_platform} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder osPlatform(String value) {
            this.osPlatform = value;
            return this;
        }

        /**
         * Sets the {@code os_version} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder osVersion(String value) {
            this.osVersion = value;
            return this;
        }

        /**
         * Sets the {@code os_arch} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder osArch(String value) {
            this.osArch = value;
            return this;
        }

        /**
         * Sets the {@code node_version} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder nodeVersion(String value) {
            this.nodeVersion = value;
            return this;
        }

        /**
         * Sets the {@code copilot_plan} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder copilotPlan(String value) {
            this.copilotPlan = value;
            return this;
        }

        /**
         * Sets the {@code client_type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder clientType(String value) {
            this.clientType = value;
            return this;
        }

        /**
         * Sets the {@code client_name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder clientName(String value) {
            this.clientName = value;
            return this;
        }

        /**
         * Sets the {@code is_staff} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder isStaff(Boolean value) {
            this.isStaff = value;
            return this;
        }

        /**
         * Sets the {@code dev_device_id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder devDeviceId(String value) {
            this.devDeviceId = value;
            return this;
        }

        /**
         * Sets the {@code cpu_model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder cpuModel(String value) {
            this.cpuModel = value;
            return this;
        }

        /**
         * Sets the {@code cpu_count} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder cpuCount(Long value) {
            this.cpuCount = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubTelemetryClientInfo build() {
            if (cliVersion == null) throw new IllegalStateException("Missing required property: cli_version");
            if (osPlatform == null) throw new IllegalStateException("Missing required property: os_platform");
            if (osVersion == null) throw new IllegalStateException("Missing required property: os_version");
            if (osArch == null) throw new IllegalStateException("Missing required property: os_arch");
            if (nodeVersion == null) throw new IllegalStateException("Missing required property: node_version");
            return new GitHubTelemetryClientInfo(cliVersion, osPlatform, osVersion, osArch, nodeVersion, copilotPlan, clientType, clientName, isStaff, devDeviceId, cpuModel, cpuCount);
        }
    }
}
