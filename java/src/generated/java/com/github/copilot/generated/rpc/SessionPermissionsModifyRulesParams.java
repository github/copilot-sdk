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
import javax.annotation.processing.Generated;

/**
 * Scope and add/remove instructions for modifying session- or location-scoped permission rules.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionPermissionsModifyRulesParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Whether the change applies to ephemeral session-scoped rules (cleared at session end) or to location-scoped rules persisted via the location-permissions config file. */
    @JsonProperty("scope") PermissionsModifyRulesScope scope,
    /** Rules to add to the scope. Applied before `remove`/`removeAll`. */
    @JsonProperty("add") List<PermissionRule> add,
    /** Specific rules to remove from the scope. Ignored when `removeAll` is true. */
    @JsonProperty("remove") List<PermissionRule> remove,
    /** When true, removes every rule currently in the scope (after any `add` is applied). Useful for clearing the location scope wholesale. */
    @JsonProperty("removeAll") Boolean removeAll
) {
}
