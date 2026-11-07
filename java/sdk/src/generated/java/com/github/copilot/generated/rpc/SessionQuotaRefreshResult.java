/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * The incumbent session-owned quota and account projection.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionQuotaRefreshResult(
    /** Known quota snapshots, keyed by the provider's quota type. */
    @JsonProperty("snapshots") Map<String, SessionQuotaSnapshot> snapshots,
    /** Provider-owned account quota state, including snapshots without numeric balances. */
    @JsonProperty("providerQuotas") List<ProviderQuotaState> providerQuotas,
    /** Whether the account uses the free limited Copilot plan. */
    @JsonProperty("isFreeUser") Boolean isFreeUser,
    /** Whether the account uses token-based billing. */
    @JsonProperty("isTbbUser") Boolean isTbbUser,
    /** Resolved Copilot plan tier. */
    @JsonProperty("planTier") SessionQuotaPlanTier planTier,
    /** Whether premium-request costs are billable. */
    @JsonProperty("premiumRequestsBillable") Boolean premiumRequestsBillable,
    /** Whether model cost columns should be shown. */
    @JsonProperty("modelCostColumnVisible") Boolean modelCostColumnVisible,
    /** Whether the account can delegate tasks to GitHub. */
    @JsonProperty("delegateAvailable") Boolean delegateAvailable,
    /** Whether the account can sign up for Copilot Free. */
    @JsonProperty("canSignupForCopilotFree") Boolean canSignupForCopilotFree,
    /** Whether dynamic workflows are enabled for the active account. */
    @JsonProperty("dynamicWorkflowsEnabled") Boolean dynamicWorkflowsEnabled,
    /** Whether dynamic workflows are visible under the session's feature flags. */
    @JsonProperty("dynamicWorkflowsUiVisible") Boolean dynamicWorkflowsUiVisible,
    /** Upgrade link for a free account. */
    @JsonProperty("upgradeUrl") String upgradeUrl,
    /** Existing delegation warning for a free account. */
    @JsonProperty("delegateWarning") SessionQuotaDelegateWarning delegateWarning
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionQuotaRefreshResult {}

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
        private Map<String, SessionQuotaSnapshot> snapshots;
        private List<ProviderQuotaState> providerQuotas;
        private Boolean isFreeUser;
        private Boolean isTbbUser;
        private SessionQuotaPlanTier planTier;
        private Boolean premiumRequestsBillable;
        private Boolean modelCostColumnVisible;
        private Boolean delegateAvailable;
        private Boolean canSignupForCopilotFree;
        private Boolean dynamicWorkflowsEnabled;
        private Boolean dynamicWorkflowsUiVisible;
        private String upgradeUrl;
        private SessionQuotaDelegateWarning delegateWarning;

        /**
         * Sets the {@code snapshots} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder snapshots(Map<String, SessionQuotaSnapshot> value) {
            this.snapshots = value;
            return this;
        }

        /**
         * Sets the {@code providerQuotas} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder providerQuotas(List<ProviderQuotaState> value) {
            this.providerQuotas = value;
            return this;
        }

        /**
         * Sets the {@code isFreeUser} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isFreeUser(Boolean value) {
            this.isFreeUser = value;
            return this;
        }

        /**
         * Sets the {@code isTbbUser} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isTbbUser(Boolean value) {
            this.isTbbUser = value;
            return this;
        }

        /**
         * Sets the {@code planTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder planTier(SessionQuotaPlanTier value) {
            this.planTier = value;
            return this;
        }

        /**
         * Sets the {@code premiumRequestsBillable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder premiumRequestsBillable(Boolean value) {
            this.premiumRequestsBillable = value;
            return this;
        }

        /**
         * Sets the {@code modelCostColumnVisible} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelCostColumnVisible(Boolean value) {
            this.modelCostColumnVisible = value;
            return this;
        }

        /**
         * Sets the {@code delegateAvailable} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder delegateAvailable(Boolean value) {
            this.delegateAvailable = value;
            return this;
        }

        /**
         * Sets the {@code canSignupForCopilotFree} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder canSignupForCopilotFree(Boolean value) {
            this.canSignupForCopilotFree = value;
            return this;
        }

        /**
         * Sets the {@code dynamicWorkflowsEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder dynamicWorkflowsEnabled(Boolean value) {
            this.dynamicWorkflowsEnabled = value;
            return this;
        }

        /**
         * Sets the {@code dynamicWorkflowsUiVisible} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder dynamicWorkflowsUiVisible(Boolean value) {
            this.dynamicWorkflowsUiVisible = value;
            return this;
        }

        /**
         * Sets the {@code upgradeUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder upgradeUrl(String value) {
            this.upgradeUrl = value;
            return this;
        }

        /**
         * Sets the {@code delegateWarning} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder delegateWarning(SessionQuotaDelegateWarning value) {
            this.delegateWarning = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionQuotaRefreshResult build() {
            if (snapshots == null) throw new IllegalStateException("Missing required property: snapshots");
            if (isFreeUser == null) throw new IllegalStateException("Missing required property: isFreeUser");
            if (isTbbUser == null) throw new IllegalStateException("Missing required property: isTbbUser");
            if (planTier == null) throw new IllegalStateException("Missing required property: planTier");
            if (premiumRequestsBillable == null) throw new IllegalStateException("Missing required property: premiumRequestsBillable");
            if (modelCostColumnVisible == null) throw new IllegalStateException("Missing required property: modelCostColumnVisible");
            if (delegateAvailable == null) throw new IllegalStateException("Missing required property: delegateAvailable");
            if (canSignupForCopilotFree == null) throw new IllegalStateException("Missing required property: canSignupForCopilotFree");
            if (dynamicWorkflowsEnabled == null) throw new IllegalStateException("Missing required property: dynamicWorkflowsEnabled");
            if (dynamicWorkflowsUiVisible == null) throw new IllegalStateException("Missing required property: dynamicWorkflowsUiVisible");
            return new SessionQuotaRefreshResult(snapshots, providerQuotas, isFreeUser, isTbbUser, planTier, premiumRequestsBillable, modelCostColumnVisible, delegateAvailable, canSignupForCopilotFree, dynamicWorkflowsEnabled, dynamicWorkflowsUiVisible, upgradeUrl, delegateWarning);
        }
    }
}
