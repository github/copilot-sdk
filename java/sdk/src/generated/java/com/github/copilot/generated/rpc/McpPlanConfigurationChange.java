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
 * One change applying the plan would make, described rather than serialised so the configuration payload stays behind the runtime boundary.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpPlanConfigurationChange(
    /** Whether the change would create a new entry or modify an existing one. */
    @JsonProperty("operation") McpPlanConfigurationOperation operation,
    /** Scope the change would be written to. */
    @JsonProperty("scope") McpPlanScope scope,
    /** Configuration key the change applies to. */
    @JsonProperty("configKey") String configKey,
    /** Names of the configuration fields the change would set, without their values. */
    @JsonProperty("changedFields") List<String> changedFields,
    /** Secret placeholders the written configuration would reference. The constrained placeholder type cannot carry a literal secret value. */
    @JsonProperty("secretReferences") List<String> secretReferences
) {
}
