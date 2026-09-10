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
 * Client-owned, case-sensitive string metadata persisted with a local session. Clients should namespace keys by owner. Keys must be non-empty and at most 256 UTF-8 bytes; keys under `copilot/` and `github/` are reserved. Values may contain at most 16 KiB of UTF-8 data. A bag may contain at most 128 entries and its serialized sidecar may contain at most 64 KiB. The runtime stores but never interprets these values.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClientMetadata() {
}
