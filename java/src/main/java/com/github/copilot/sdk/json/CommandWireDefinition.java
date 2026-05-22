/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire-format representation of a command definition for RPC serialization.
 * <p>
 * This is a low-level class used internally. Use {@link CommandDefinition} to
 * define commands for a session.
 *
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class CommandWireDefinition {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    /** Creates an empty definition. */
    public CommandWireDefinition() {
    }

    /** Creates a definition with name and description. */
    public CommandWireDefinition(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /** Gets the command name. @return the name */
    public String getName() {
        return name;
    }

    /** Sets the command name. @param name the name @return this */
    public CommandWireDefinition setName(String name) {
        this.name = name;
        return this;
    }

    /** Gets the description. @return the description */
    public String getDescription() {
        return description;
    }

    /** Sets the description. @param description the description @return this */
    public CommandWireDefinition setDescription(String description) {
        this.description = description;
        return this;
    }
}
