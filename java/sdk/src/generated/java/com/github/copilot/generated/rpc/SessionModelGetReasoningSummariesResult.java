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
import javax.annotation.processing.Generated;

/**
 * Reasoning-summaries presentation policy for the session's current model.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionModelGetReasoningSummariesResult(
    /** False until the session has a model catalog and a selected model. This read never resolves the catalog itself, and the runtime emits no readiness notification, so repeating this call alone leaves `ready` false indefinitely on a session that has neither listed models nor run a turn. `session.model.list` is the prerequisite and the signal: it resolves and caches the catalog for the session, so a successful return means the next read is ready. The exception is a session that resolves models without a Copilot catalog, that is a BYOK registry or an alternate provider: `session.model.list` returns an empty list without caching a catalog, so `ready` stays false and this policy is unavailable there. While false, every model-policy field is false and `visibility` is omitted; `experimentSettled` is independent of readiness and still reports the assignment state. */
    @JsonProperty("ready") Boolean ready,
    /** Whether the session's reasoning-summaries experiment assignment has settled. False means this read does not wait for a pending assignment and reports the experiment-derived fields conservatively: `showHiddenHint` is false and `visibility` falls back to `persistedShowReasoning`, or is omitted when none was supplied. A host that wants the assigned answer re-reads until this is true. */
    @JsonProperty("experimentSettled") Boolean experimentSettled,
    /** Whether turns on this session produce reasoning summaries, honouring an explicit session reasoning-summary override. */
    @JsonProperty("sessionShowsSummaries") Boolean sessionShowsSummaries,
    /** Whether the selected model shows reasoning summaries by default. */
    @JsonProperty("modelDefaultShowsSummaries") Boolean modelDefaultShowsSummaries,
    /** Whether the selected model emits reasoning headers. */
    @JsonProperty("showReasoningHeaders") Boolean showReasoningHeaders,
    /** Default reasoning visibility, and only meaningful once `ready` is true: false when the session produces no summaries, else the persisted setting, else the experiment's default. While `ready` is true it is omitted only when all three are unavailable, that is when the session produces summaries, no `persistedShowReasoning` was supplied, and `experimentSettled` is false. While `ready` is false there is no model policy to report and this field is always omitted, so an absent value alone does not imply a pending experiment. */
    @JsonProperty("visibility") Boolean visibility,
    /** Whether to show the hint that reasoning summaries are hidden. Always false while `experimentSettled` is false. */
    @JsonProperty("showHiddenHint") Boolean showHiddenHint,
    /** Whether the supplied `persistedShowReasoning` setting only restates the model default. This reports a redundancy, not a standing instruction to delete the setting: it stays true for as long as the setting and the model default agree, including for a user who later chose that value deliberately. It exists for a one-time migration of a legacy setting written before the model default made it redundant, so a host that acts on it owns the cleanup and must gate it behind its own persisted `done` marker, delete only a setting whose value still matches the one it read, and never repeat the deletion on a later launch. Under an off-by-default reasoning-summaries experiment an explicit `showReasoning: true` opt-in also reads as redundant on a summary-producing model, so an ungated host would keep deleting that opt-in and hide summaries again on every launch. */
    @JsonProperty("clearRedundantSetting") Boolean clearRedundantSetting
) {
}
