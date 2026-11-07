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
 * Scope and add/remove instructions for modifying session- or location-scoped permission rules.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
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

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionPermissionsModifyRulesParams {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String sessionId;
        private PermissionsModifyRulesScope scope;
        private List<PermissionRule> add;
        private List<PermissionRule> remove;
        private Boolean removeAll;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder scope(PermissionsModifyRulesScope value) {
            this.scope = value;
            return this;
        }

        /**
         * Sets the {@code add} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder add(List<PermissionRule> value) {
            this.add = value;
            return this;
        }

        /**
         * Sets the {@code remove} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder remove(List<PermissionRule> value) {
            this.remove = value;
            return this;
        }

        /**
         * Sets the {@code removeAll} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder removeAll(Boolean value) {
            this.removeAll = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionPermissionsModifyRulesParams build() {
            if (scope == null) throw new IllegalStateException("Missing required property: scope");
            return new SessionPermissionsModifyRulesParams(sessionId, scope, add, remove, removeAll);
        }
    }
}
