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
 * Entries in the requested directory paired with file/directory type information, or a filesystem error if the read failed.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsReaddirWithTypesResult(
    /** Directory entries with type information */
    @JsonProperty("entries") List<SessionFsReaddirWithTypesEntry> entries,
    /** Describes a filesystem error. */
    @JsonProperty("error") SessionFsError error
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFsReaddirWithTypesResult {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private List<SessionFsReaddirWithTypesEntry> entries;
        private SessionFsError error;

        /**
         * Sets the {@code entries} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder entries(List<SessionFsReaddirWithTypesEntry> value) {
            this.entries = value;
            return this;
        }

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder error(SessionFsError value) {
            this.error = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionFsReaddirWithTypesResult build() {
            if (entries == null) throw new IllegalStateException("Missing required property: entries");
            return new SessionFsReaddirWithTypesResult(entries, error);
        }
    }
}
