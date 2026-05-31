/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Schema for the `PermissionDeniedByPermissionRequestHook` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionDeniedByPermissionRequestHook extends PermissionResult {

    @JsonProperty("kind")
    private final String kind = "denied-by-permission-request-hook";

    @Override
    public String getKind() { return kind; }

    /** Optional message from the hook explaining the denial */
    @JsonProperty("message")
    private String message;

    /** Whether to interrupt the current agent turn */
    @JsonProperty("interrupt")
    private Boolean interrupt;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getInterrupt() { return interrupt; }
    public void setInterrupt(Boolean interrupt) { this.interrupt = interrupt; }
}
