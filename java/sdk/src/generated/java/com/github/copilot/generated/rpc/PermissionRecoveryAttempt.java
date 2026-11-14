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

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionRecoveryAttempt(
    /** Unique identifier for this attempt record */
    @JsonProperty("attemptId") String attemptId,
    /** Tool-call identifier associated with this attempt, when available */
    @JsonProperty("toolCallId") String toolCallId,
    /** Controlled permission request kind, such as shell, path, URL, or tool */
    @JsonProperty("permissionKind") String permissionKind,
    /** SHA-256 fingerprint of normalized request data; raw permission arguments are not included */
    @JsonProperty("requestFingerprint") String requestFingerprint,
    /** Relationship between this attempt and earlier attempts in the episode */
    @JsonProperty("relation") PermissionRecoveryAttemptRelation relation,
    /** How the runtime handled this attempt */
    @JsonProperty("disposition") PermissionRecoveryAttemptDisposition disposition,
    /** Controlled reason for the attempt disposition */
    @JsonProperty("reason") PermissionRecoveryAttemptReason reason,
    /** One-based position of this attempt in the episode */
    @JsonProperty("ordinal") Long ordinal
) {
}
