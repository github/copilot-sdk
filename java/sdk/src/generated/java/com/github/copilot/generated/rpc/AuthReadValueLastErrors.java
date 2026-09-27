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
import javax.annotation.processing.Generated;

/**
 * Variant {@code lastErrors} of {@link AuthReadValue}.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AuthReadValueLastErrors extends AuthReadValue {

    @JsonProperty("kind")
    private final String kind = "lastErrors";

    @Override
    public String getKind() { return kind; }

    /** Validation errors from the most recent authentication attempt. */
    @JsonProperty("errors")
    private List<AuthValidationError> errors;

    public List<AuthValidationError> getErrors() { return errors; }
    public void setErrors(List<AuthValidationError> errors) { this.errors = errors; }
}
