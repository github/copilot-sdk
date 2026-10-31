/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The latest unresolved Policy Helper failure for this policy context. Helper-authored text is bounded display-intended content but remains untrusted; render it as plain text, keep diagnostics secondary, and do not automatically log, persist, export, execute, or inject it into model context.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyHelperFailure(
    /** Stable runtime classification. Known values are helper-reported, registration-undetermined, working-directory, resolve, resolve-task, spawn, timeout, exit-status, output-too-large, envelope, schema, and internal. This is not a closed set; clients must tolerate unknown values. */
    @JsonProperty("category") String category,
    /** Effective runtime behavior: ignore continues with remaining or retained policy, while failClosed activates restrictive safeguards. This is runtime-owned, not helper-authored. */
    @JsonProperty("failureMode") String failureMode,
    /** Runtime-calculated presentation severity: warning for ignore and error for failClosed. Error severity does not imply that session creation failed. */
    @JsonProperty("severity") String severity,
    /** Invocation reason. Known values are sessionStart and pluginActivation; clients must tolerate future values. */
    @JsonProperty("reason") String reason,
    /** Managed authority that registered the helper, when known. Known values are device and server; clients must tolerate future values. */
    @JsonProperty("authority") String authority,
    /** True when an earlier successful helper-derived layer still contributes to this effective snapshot. This does not promise that every relaxation from the old layer was retained. */
    @JsonProperty("previousPolicyRetained") Boolean previousPolicyRetained,
    /** Stable bounded helper-defined identifier for a valid structured helper error. Absent for runtime-detected failures and not globally unique. */
    @JsonProperty("code") String code,
    /** Bounded plain-text primary explanation. Runtime-authored for runtime failures and helper-authored for a valid structured helper error. */
    @JsonProperty("message") String message,
    /** Optional bounded helper-authored plain-text recovery guidance. It is explanatory and must not be executed automatically. */
    @JsonProperty("remediation") String remediation,
    /** Optional explicit validated additional-information link. URLs embedded in other text are not links. */
    @JsonProperty("helpLink") PolicyHelperHelpLink helpLink,
    /** Optional bounded field-specific findings supplied by the helper. */
    @JsonProperty("details") List<PolicyHelperFailureDetail> details,
    /** Optional bounded untrusted secondary troubleshooting text, such as trimmed stderr. Do not use it as primary UI copy. */
    @JsonProperty("diagnostic") String diagnostic
) {
}
