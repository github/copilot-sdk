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
    /** GitHub login of the authenticated user. */
    @JsonProperty("login") String login,
    /** Copilot access SKU identifier (e.g. `free_limited_copilot`, `copilot_for_business_seat_quota`) used to gate model and feature access. */
    @JsonProperty("access_type_sku") String accessTypeSku,
    /** Opaque analytics tracking identifier for the user, forwarded from the Copilot API. */
    @JsonProperty("analytics_tracking_id") String analyticsTrackingId,
    /** Date the Copilot seat was assigned to the user, if applicable. */
    @JsonProperty("assigned_date") Object assignedDate,
    /** Whether the user is eligible to sign up for the free/limited Copilot tier. */
    @JsonProperty("can_signup_for_limited") Boolean canSignupForLimited,
    /** Whether Copilot chat is enabled for the user. */
    @JsonProperty("chat_enabled") Boolean chatEnabled,
    /** Copilot plan name for the user (e.g. `individual`, `business`, `enterprise`). */
    @JsonProperty("copilot_plan") String copilotPlan,
    /** Whether `.copilotignore` content-exclusion support is enabled for the user. */
    @JsonProperty("copilotignore_enabled") Boolean copilotignoreEnabled,
    /** Endpoint URLs from the raw Copilot `/copilot_internal/v2/token` user-response passthrough. */
    @JsonProperty("endpoints") CopilotUserResponseEndpoints endpoints,
    /** Logins of the organizations the user belongs to. */
    @JsonProperty("organization_login_list") List<String> organizationLoginList,
    /** Organizations the user belongs to, each with an optional login and display name. */
    @JsonProperty("organization_list") Object organizationList,
    /** Whether the Codex agent is enabled for the user. */
    @JsonProperty("codex_agent_enabled") Boolean codexAgentEnabled,
    /** Whether MCP (Model Context Protocol) support is enabled for the user. */
    @JsonProperty("is_mcp_enabled") Object isMcpEnabled,
    /** Date the user's usage quota next resets, as a raw string from the Copilot API; see `quota_reset_date_utc` for the UTC-normalized value. */
    @JsonProperty("quota_reset_date") String quotaResetDate,
    /** Quota snapshot map from the raw Copilot user-response passthrough, with chat, completions, premium-interactions, and other entries. */
    @JsonProperty("quota_snapshots") CopilotUserResponseQuotaSnapshots quotaSnapshots,
    /** Whether the user's telemetry is subject to restricted-data handling. */
    @JsonProperty("restricted_telemetry") Boolean restrictedTelemetry,
    /** Whether the user is a GitHub/Microsoft staff member. */
    @JsonProperty("is_staff") Boolean isStaff,
    /** Raw passthrough of the Copilot API `te` flag for the user (an opaque server-side eligibility signal surfaced in telemetry); not otherwise interpreted by the runtime. */
    @JsonProperty("te") Boolean te,
    /** Whether the account is on usage-based (token/AI-credit) billing rather than a fixed premium-request quota. */
    @JsonProperty("token_based_billing") Boolean tokenBasedBilling,
    /** Whether the user is able to upgrade their Copilot plan. */
    @JsonProperty("can_upgrade_plan") Boolean canUpgradePlan,
    /** UTC-normalized form of `quota_reset_date` (the date the user's usage quota next resets). */
    @JsonProperty("quota_reset_date_utc") String quotaResetDateUtc,
    /** Per-category quota allotments for free/limited-tier users, keyed by quota category. */
    @JsonProperty("limited_user_quotas") Map<String, Double> limitedUserQuotas,
    /** Date the free/limited-tier user's quotas next reset, as a raw string from the Copilot API. */
    @JsonProperty("limited_user_reset_date") String limitedUserResetDate,
    /** Per-category monthly quota allotments, keyed by quota category. */
    @JsonProperty("monthly_quotas") Map<String, Double> monthlyQuotas,
    /** Whether cloud session storage is enabled for the user. */
    @JsonProperty("cloud_session_storage_enabled") Boolean cloudSessionStorageEnabled,
    /** Whether CLI remote control is enabled for the user. */
    @JsonProperty("cli_remote_control_enabled") Boolean cliRemoteControlEnabled
) {
}
