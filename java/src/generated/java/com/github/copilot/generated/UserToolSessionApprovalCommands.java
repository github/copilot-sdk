/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Schema for the `UserToolSessionApprovalCommands` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class UserToolSessionApprovalCommands extends UserToolSessionApproval {

    @JsonProperty("kind")
    private final String kind = "commands";

    @Override
    public String getKind() { return kind; }

    /** Command identifiers approved by the user */
    @JsonProperty("commandIdentifiers")
    private List<String> commandIdentifiers;

    public List<String> getCommandIdentifiers() { return commandIdentifiers; }
    public void setCommandIdentifiers(List<String> commandIdentifiers) { this.commandIdentifiers = commandIdentifiers; }
}
