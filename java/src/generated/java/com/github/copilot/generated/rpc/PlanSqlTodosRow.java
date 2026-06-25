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

/**
 * A single todo row read from the session SQL `todos` table. All fields are optional because the SQL schema is best-effort and the agent may not have populated every column.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlanSqlTodosRow(
    /** Todo identifier. */
    @JsonProperty("id") String id,
    /** Todo title. */
    @JsonProperty("title") String title,
    /** Todo description. */
    @JsonProperty("description") String description,
    /** Todo status. */
    @JsonProperty("status") String status
) {
}
