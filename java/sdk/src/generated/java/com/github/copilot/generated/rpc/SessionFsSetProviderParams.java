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
import javax.annotation.processing.Generated;

/**
 * Initial working directory, session-state path layout, and path conventions used to register the calling SDK client as the session filesystem provider. A registered provider is authoritative for path interpretation and filesystem facts used by workspace permission validation. Paths are interpreted lexically; home-relative paths (`~` and `~/...`) and Windows drive-relative paths such as `C:foo` are unsupported. Until provider-side canonicalization is supported, providers must not expose symlinks inside allowed roots that escape those roots.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionFsSetProviderParams(
    /** Absolute initial working directory for sessions. Registering the provider establishes this path as the root of its virtual namespace; the runtime does not require the provider to materialize or stat it before creating a session. */
    @JsonProperty("initialCwd") String initialCwd,
    /** Path within each session's SessionFs where the runtime stores files for that session */
    @JsonProperty("sessionStatePath") String sessionStatePath,
    /** Path conventions used by this filesystem */
    @JsonProperty("conventions") SessionFsSetProviderConventions conventions,
    /** Optional capabilities declared by the provider */
    @JsonProperty("capabilities") SessionFsSetProviderCapabilities capabilities
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionFsSetProviderParams {}

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
        private String initialCwd;
        private String sessionStatePath;
        private SessionFsSetProviderConventions conventions;
        private SessionFsSetProviderCapabilities capabilities;

        /**
         * Sets the {@code initialCwd} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder initialCwd(String value) {
            this.initialCwd = value;
            return this;
        }

        /**
         * Sets the {@code sessionStatePath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionStatePath(String value) {
            this.sessionStatePath = value;
            return this;
        }

        /**
         * Sets the {@code conventions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder conventions(SessionFsSetProviderConventions value) {
            this.conventions = value;
            return this;
        }

        /**
         * Sets the {@code capabilities} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder capabilities(SessionFsSetProviderCapabilities value) {
            this.capabilities = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionFsSetProviderParams build() {
            if (initialCwd == null) throw new IllegalStateException("Missing required property: initialCwd");
            if (sessionStatePath == null) throw new IllegalStateException("Missing required property: sessionStatePath");
            if (conventions == null) throw new IllegalStateException("Missing required property: conventions");
            return new SessionFsSetProviderParams(initialCwd, sessionStatePath, conventions, capabilities);
        }
    }
}
