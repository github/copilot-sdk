/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from the status.get RPC call.
 * <p>
 * Contains information about the CLI version and protocol version.
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
}
