/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Query results including rows, columns, and rows affected, or a filesystem error if execution failed.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsSqliteQueryResult(
    /** For SELECT: array of row objects. For others: empty array. */
    @JsonProperty("rows") List<Map<String, Object>> rows,
    /** Column names from the result set */
    @JsonProperty("columns") List<String> columns,
    /** Number of rows affected (for INSERT/UPDATE/DELETE) */
    @JsonProperty("rowsAffected") Long rowsAffected,
    /** Last inserted row ID (for INSERT) */
    @JsonProperty("lastInsertRowid") Double lastInsertRowid,
    /** Describes a filesystem error. */
    @JsonProperty("error") SessionFsError error
) {
}
