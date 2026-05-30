/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CopilotUserResponse(
    @JsonProperty("login") String login,
    @JsonProperty("access_type_sku") String accessTypeSku,
    @JsonProperty("analytics_tracking_id") String analyticsTrackingId,
    @JsonProperty("assigned_date") Object assignedDate,
    @JsonProperty("can_signup_for_limited") Boolean canSignupForLimited,
    @JsonProperty("chat_enabled") Boolean chatEnabled,
    @JsonProperty("copilot_plan") String copilotPlan,
    @JsonProperty("copilotignore_enabled") Boolean copilotignoreEnabled,
    /** Schema for the `CopilotUserResponseEndpoints` type. */
    @JsonProperty("endpoints") CopilotUserResponseEndpoints endpoints,
    @JsonProperty("organization_login_list") List<String> organizationLoginList,
    @JsonProperty("organization_list") Object organizationList,
    @JsonProperty("codex_agent_enabled") Boolean codexAgentEnabled,
    @JsonProperty("is_mcp_enabled") Object isMcpEnabled,
    @JsonProperty("quota_reset_date") String quotaResetDate,
    /** Schema for the `CopilotUserResponseQuotaSnapshots` type. */
    @JsonProperty("quota_snapshots") CopilotUserResponseQuotaSnapshots quotaSnapshots,
    @JsonProperty("restricted_telemetry") Boolean restrictedTelemetry,
    @JsonProperty("token_based_billing") Boolean tokenBasedBilling,
    @JsonProperty("quota_reset_date_utc") String quotaResetDateUtc,
    @JsonProperty("limited_user_quotas") Map<String, Double> limitedUserQuotas,
    @JsonProperty("limited_user_reset_date") String limitedUserResetDate,
    @JsonProperty("monthly_quotas") Map<String, Double> monthlyQuotas,
    @JsonProperty("cloud_session_storage_enabled") Boolean cloudSessionStorageEnabled,
    @JsonProperty("cli_remote_control_enabled") Boolean cliRemoteControlEnabled
) {
}
