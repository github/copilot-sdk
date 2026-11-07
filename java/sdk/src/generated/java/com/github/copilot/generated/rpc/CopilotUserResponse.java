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
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = CopilotUserResponse.$NullableDeserializer.class)
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
    /** Organizations the user belongs to, each with an optional ID, login, and display name. */
    @JsonProperty("organization_list") Object organizationList,
    /** Enterprises that provide the user's Copilot license; malformed entries are normalized to null or ID-less shapes. */
    @JsonProperty("enterprise_list") List<CopilotUserResponseEnterpriseListItem> enterpriseList,
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
    @JsonProperty("cli_remote_control_enabled") Boolean cliRemoteControlEnabled,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CopilotUserResponseEnterpriseListItem(
        /** JavaScript-safe numeric database ID of the enterprise. */
        @JsonProperty("id") Long id
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public CopilotUserResponseEnterpriseListItem {}

        /**
         * Returns a named builder.
         * @return a new builder
         * @since 2.0.0
         */
        public static Builder builder() { return new Builder(); }

        /**
         * Named construction for this record.
         * @since 2.0.0
         */
        public static final class Builder {
            private Builder() {}
            private Long id;

            /**
             * Sets the {@code id} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder id(Long value) {
                this.id = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public CopilotUserResponseEnterpriseListItem build() {
                return new CopilotUserResponseEnterpriseListItem(id);
            }
        }
    }

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CopilotUserResponse { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CopilotUserResponse(String login, String accessTypeSku, String analyticsTrackingId, Object assignedDate, Boolean canSignupForLimited, Boolean chatEnabled, String copilotPlan, Boolean copilotignoreEnabled, CopilotUserResponseEndpoints endpoints, List<String> organizationLoginList, Object organizationList, List<CopilotUserResponseEnterpriseListItem> enterpriseList, Boolean codexAgentEnabled, Object isMcpEnabled, String quotaResetDate, CopilotUserResponseQuotaSnapshots quotaSnapshots, Boolean restrictedTelemetry, Boolean isStaff, Boolean te, Boolean tokenBasedBilling, Boolean canUpgradePlan, String quotaResetDateUtc, Map<String, Double> limitedUserQuotas, String limitedUserResetDate, Map<String, Double> monthlyQuotas, Boolean cloudSessionStorageEnabled, Boolean cliRemoteControlEnabled) {
        this(login, accessTypeSku, analyticsTrackingId, assignedDate, canSignupForLimited, chatEnabled, copilotPlan, copilotignoreEnabled, endpoints, organizationLoginList, organizationList, enterpriseList, codexAgentEnabled, isMcpEnabled, quotaResetDate, quotaSnapshots, restrictedTelemetry, isStaff, te, tokenBasedBilling, canUpgradePlan, quotaResetDateUtc, limitedUserQuotas, limitedUserResetDate, monthlyQuotas, cloudSessionStorageEnabled, cliRemoteControlEnabled, java.util.Set.of());
    }

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private String login;
        private String accessTypeSku;
        private String analyticsTrackingId;
        private Object assignedDate;
        private boolean $presence3;
        private Boolean canSignupForLimited;
        private Boolean chatEnabled;
        private String copilotPlan;
        private Boolean copilotignoreEnabled;
        private CopilotUserResponseEndpoints endpoints;
        private List<String> organizationLoginList;
        private Object organizationList;
        private boolean $presence10;
        private List<CopilotUserResponseEnterpriseListItem> enterpriseList;
        private boolean $presence11;
        private Boolean codexAgentEnabled;
        private Object isMcpEnabled;
        private boolean $presence13;
        private String quotaResetDate;
        private CopilotUserResponseQuotaSnapshots quotaSnapshots;
        private Boolean restrictedTelemetry;
        private Boolean isStaff;
        private Boolean te;
        private Boolean tokenBasedBilling;
        private Boolean canUpgradePlan;
        private String quotaResetDateUtc;
        private Map<String, Double> limitedUserQuotas;
        private String limitedUserResetDate;
        private Map<String, Double> monthlyQuotas;
        private Boolean cloudSessionStorageEnabled;
        private Boolean cliRemoteControlEnabled;

        /**
         * Sets the {@code login} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder login(String value) {
            this.login = value;
            return this;
        }

        /**
         * Sets the {@code access_type_sku} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder accessTypeSku(String value) {
            this.accessTypeSku = value;
            return this;
        }

        /**
         * Sets the {@code analytics_tracking_id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder analyticsTrackingId(String value) {
            this.analyticsTrackingId = value;
            return this;
        }

        /**
         * Sets the {@code assigned_date} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder assignedDate(Object value) {
            this.assignedDate = value;
            this.$presence3 = true;
            return this;
        }

        /**
         * Sets the {@code can_signup_for_limited} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder canSignupForLimited(Boolean value) {
            this.canSignupForLimited = value;
            return this;
        }

        /**
         * Sets the {@code chat_enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder chatEnabled(Boolean value) {
            this.chatEnabled = value;
            return this;
        }

        /**
         * Sets the {@code copilot_plan} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder copilotPlan(String value) {
            this.copilotPlan = value;
            return this;
        }

        /**
         * Sets the {@code copilotignore_enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder copilotignoreEnabled(Boolean value) {
            this.copilotignoreEnabled = value;
            return this;
        }

        /**
         * Sets the {@code endpoints} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder endpoints(CopilotUserResponseEndpoints value) {
            this.endpoints = value;
            return this;
        }

        /**
         * Sets the {@code organization_login_list} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder organizationLoginList(List<String> value) {
            this.organizationLoginList = value;
            return this;
        }

        /**
         * Sets the {@code organization_list} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder organizationList(Object value) {
            this.organizationList = value;
            this.$presence10 = true;
            return this;
        }

        /**
         * Sets the {@code enterprise_list} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enterpriseList(List<CopilotUserResponseEnterpriseListItem> value) {
            this.enterpriseList = value;
            this.$presence11 = true;
            return this;
        }

        /**
         * Sets the {@code codex_agent_enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder codexAgentEnabled(Boolean value) {
            this.codexAgentEnabled = value;
            return this;
        }

        /**
         * Sets the {@code is_mcp_enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isMcpEnabled(Object value) {
            this.isMcpEnabled = value;
            this.$presence13 = true;
            return this;
        }

        /**
         * Sets the {@code quota_reset_date} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder quotaResetDate(String value) {
            this.quotaResetDate = value;
            return this;
        }

        /**
         * Sets the {@code quota_snapshots} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder quotaSnapshots(CopilotUserResponseQuotaSnapshots value) {
            this.quotaSnapshots = value;
            return this;
        }

        /**
         * Sets the {@code restricted_telemetry} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder restrictedTelemetry(Boolean value) {
            this.restrictedTelemetry = value;
            return this;
        }

        /**
         * Sets the {@code is_staff} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isStaff(Boolean value) {
            this.isStaff = value;
            return this;
        }

        /**
         * Sets the {@code te} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder te(Boolean value) {
            this.te = value;
            return this;
        }

        /**
         * Sets the {@code token_based_billing} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tokenBasedBilling(Boolean value) {
            this.tokenBasedBilling = value;
            return this;
        }

        /**
         * Sets the {@code can_upgrade_plan} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder canUpgradePlan(Boolean value) {
            this.canUpgradePlan = value;
            return this;
        }

        /**
         * Sets the {@code quota_reset_date_utc} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder quotaResetDateUtc(String value) {
            this.quotaResetDateUtc = value;
            return this;
        }

        /**
         * Sets the {@code limited_user_quotas} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder limitedUserQuotas(Map<String, Double> value) {
            this.limitedUserQuotas = value;
            return this;
        }

        /**
         * Sets the {@code limited_user_reset_date} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder limitedUserResetDate(String value) {
            this.limitedUserResetDate = value;
            return this;
        }

        /**
         * Sets the {@code monthly_quotas} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder monthlyQuotas(Map<String, Double> value) {
            this.monthlyQuotas = value;
            return this;
        }

        /**
         * Sets the {@code cloud_session_storage_enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cloudSessionStorageEnabled(Boolean value) {
            this.cloudSessionStorageEnabled = value;
            return this;
        }

        /**
         * Sets the {@code cli_remote_control_enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cliRemoteControlEnabled(Boolean value) {
            this.cliRemoteControlEnabled = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CopilotUserResponse build() {
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence3 && assignedDate == null) $explicitNulls.add("assigned_date");
            if ($presence10 && organizationList == null) $explicitNulls.add("organization_list");
            if ($presence11 && enterpriseList == null) $explicitNulls.add("enterprise_list");
            if ($presence13 && isMcpEnabled == null) $explicitNulls.add("is_mcp_enabled");
            return new CopilotUserResponse(login, accessTypeSku, analyticsTrackingId, assignedDate, canSignupForLimited, chatEnabled, copilotPlan, copilotignoreEnabled, endpoints, organizationLoginList, organizationList, enterpriseList, codexAgentEnabled, isMcpEnabled, quotaResetDate, quotaSnapshots, restrictedTelemetry, isStaff, te, tokenBasedBilling, canUpgradePlan, quotaResetDateUtc, limitedUserQuotas, limitedUserResetDate, monthlyQuotas, cloudSessionStorageEnabled, cliRemoteControlEnabled, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("assigned_date") && assignedDate == null) values.put("assigned_date", null);
        if ($explicitNulls.contains("organization_list") && organizationList == null) values.put("organization_list", null);
        if ($explicitNulls.contains("enterprise_list") && enterpriseList == null) values.put("enterprise_list", null);
        if ($explicitNulls.contains("is_mcp_enabled") && isMcpEnabled == null) values.put("is_mcp_enabled", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<CopilotUserResponse> {
        @Override
        public CopilotUserResponse deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(CopilotUserResponse.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("assigned_date") && tree.get("assigned_date").isNull()) $explicitNulls.add("assigned_date");
            if (tree.has("organization_list") && tree.get("organization_list").isNull()) $explicitNulls.add("organization_list");
            if (tree.has("enterprise_list") && tree.get("enterprise_list").isNull()) $explicitNulls.add("enterprise_list");
            if (tree.has("is_mcp_enabled") && tree.get("is_mcp_enabled").isNull()) $explicitNulls.add("is_mcp_enabled");
            return new CopilotUserResponse(readProperty(context, tree, "login", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "access_type_sku", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "analytics_tracking_id", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "assigned_date", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "can_signup_for_limited", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "chat_enabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "copilot_plan", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "copilotignore_enabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "endpoints", new com.fasterxml.jackson.core.type.TypeReference<CopilotUserResponseEndpoints>() {}), readProperty(context, tree, "organization_login_list", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "organization_list", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "enterprise_list", new com.fasterxml.jackson.core.type.TypeReference<List<CopilotUserResponseEnterpriseListItem>>() {}), readProperty(context, tree, "codex_agent_enabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "is_mcp_enabled", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "quota_reset_date", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "quota_snapshots", new com.fasterxml.jackson.core.type.TypeReference<CopilotUserResponseQuotaSnapshots>() {}), readProperty(context, tree, "restricted_telemetry", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "is_staff", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "te", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "token_based_billing", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "can_upgrade_plan", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "quota_reset_date_utc", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "limited_user_quotas", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Double>>() {}), readProperty(context, tree, "limited_user_reset_date", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "monthly_quotas", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Double>>() {}), readProperty(context, tree, "cloud_session_storage_enabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "cli_remote_control_enabled", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
