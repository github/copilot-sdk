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
 * `child_process.spawn` itself failed before the child entered the registry.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AgentRegistrySpawnError extends AgentRegistrySpawnResult {

    @JsonProperty("kind")
    private final String kind = "spawn-error";

    @Override
    public String getKind() { return kind; }

    /** Human-readable error message */
    @JsonProperty("message")
    private String message;

    /** Underlying errno code (e.g. ENOENT, EACCES) when available */
    @JsonProperty("code")
    private String code;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
