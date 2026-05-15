/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * UI-specific capability flags for a session.
 *
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionUiCapabilities {

    @JsonProperty("elicitation")
    private Boolean elicitation;

    /**
     * Returns whether the host supports interactive elicitation dialogs.
     *
     * @return an {@link Optional} containing the boolean value, or empty if not set
     */
    @JsonIgnore
    public Optional<Boolean> getElicitation() {
        return Optional.ofNullable(elicitation);
    }

    /**
     * Sets whether the host supports interactive elicitation dialogs.
     *
     * @param elicitation
     *            {@code true} if elicitation is supported
     * @return this instance for method chaining
     */
    public SessionUiCapabilities setElicitation(boolean elicitation) {
        this.elicitation = elicitation;
        return this;
    }

    /**
     * Clears the elicitation setting, reverting to the default behavior.
     *
     * @return this instance for method chaining
     */
    public SessionUiCapabilities clearElicitation() {
        this.elicitation = null;
        return this;
    }

}
