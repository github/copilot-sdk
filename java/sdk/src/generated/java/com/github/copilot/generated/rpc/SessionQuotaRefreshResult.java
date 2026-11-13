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
}
