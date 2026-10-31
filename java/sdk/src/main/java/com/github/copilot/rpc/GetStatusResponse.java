/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.generated.rpc.HostDiagnostics;

/**
 * Response from the status.get RPC call.
 * <p>
 * Contains information about the CLI version and protocol version.
 *
 * @since 1.0.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetStatusResponse {

    /**
     * Package version (e.g., "1.0.0").
     */
    @JsonProperty("version")
    private String version;

    /**
     * Protocol version for SDK compatibility.
     */
    @JsonProperty("protocolVersion")
    private int protocolVersion;

    /**
     * Host diagnostics, when supplied by the runtime.
     */
    @JsonProperty("hostDiagnostics")
    private HostDiagnostics hostDiagnostics;

    public String getVersion() {
        return version;
    }

    public GetStatusResponse setVersion(String version) {
        this.version = version;
        return this;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public GetStatusResponse setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
        return this;
    }

    /**
     * Returns the host diagnostics, or {@code null} for older runtimes.
     *
     * @return the optional host diagnostics
     */
    public HostDiagnostics getHostDiagnostics() {
        return hostDiagnostics;
    }

    /**
     * Sets the optional host diagnostics.
     *
     * @param hostDiagnostics
     *            the host diagnostics
     * @return this response
     */
    public GetStatusResponse setHostDiagnostics(HostDiagnostics hostDiagnostics) {
        this.hostDiagnostics = hostDiagnostics;
        return this;
    }
}
