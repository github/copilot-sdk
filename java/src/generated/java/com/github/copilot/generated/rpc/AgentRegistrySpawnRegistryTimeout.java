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
 * Spawn succeeded but the child did not publish a matching managed-server entry within the timeout.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AgentRegistrySpawnRegistryTimeout extends AgentRegistrySpawnResult {

    @JsonProperty("kind")
    private final String kind = "registry-timeout";

    @Override
    public String getKind() { return kind; }

    /** Process ID of the orphaned child (so the caller can offer 'kill the pid' guidance) */
    @JsonProperty("childPid")
    private Long childPid;

    /** Per-spawn log-capture outcome; populated from spawnLiveTarget. */
    @JsonProperty("logCapture")
    private AgentRegistryLogCapture logCapture;

    public Long getChildPid() { return childPid; }
    public void setChildPid(Long childPid) { this.childPid = childPid; }

    public AgentRegistryLogCapture getLogCapture() { return logCapture; }
    public void setLogCapture(AgentRegistryLogCapture logCapture) { this.logCapture = logCapture; }
}
