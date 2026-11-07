/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Session event "session.start". Session initialization metadata including context and configuration
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionStartEvent extends SessionEvent {

    @Override
    public String getType() { return "session.start"; }

    @JsonProperty("data")
    private SessionStartEventData data;

    public SessionStartEventData getData() { return data; }
    public void setData(SessionStartEventData data) { this.data = data; }

    /** Data payload for {@link SessionStartEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionStartEventData.$NullableDeserializer.class)
    public record SessionStartEventData(
        /** Unique identifier for the session */
        @JsonProperty("sessionId") String sessionId,
        /** Schema version number for the session event format */
        @JsonProperty("version") Long version,
        /** Identifier of the software producing the events (e.g., "copilot-agent") */
        @JsonProperty("producer") String producer,
        /** Version string of the Copilot application */
        @JsonProperty("copilotVersion") String copilotVersion,
        /** ISO 8601 timestamp when the session was created */
        @JsonProperty("startTime") OffsetDateTime startTime,
        /** Model selected at session creation time, if any */
        @JsonProperty("selectedModel") String selectedModel,
        /** Provider of selectedModel at creation time, when explicitly selected. */
        @JsonProperty("providerId") String providerId,
        /** Reasoning effort level used for model calls, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max") */
        @JsonProperty("reasoningEffort") String reasoningEffort,
        /** Model that owns effort embedded in an authored model selection. Omitted for independent reasoning-effort overrides and legacy events. */
        @JsonProperty("reasoningEffortModel") String reasoningEffortModel,
        /** True when the reasoning effort is a managed-policy default bound to reasoningEffortModel. Omitted for agent-authored, user-authored, independent, and legacy effort. */
        @JsonProperty("reasoningEffortManaged") Boolean reasoningEffortManaged,
        /** Reasoning summary mode used for model calls, if applicable (e.g. "none", "concise", "detailed") */
        @JsonProperty("reasoningSummary") ReasoningSummary reasoningSummary,
        /** Output verbosity level used for model calls, if applicable (e.g. "low", "medium", "high") */
        @JsonProperty("verbosity") Verbosity verbosity,
        /** Context tier selected at session creation time for models with tiered context pricing; null when no tier is selected (e.g., non-tiered model) */
        @JsonProperty("contextTier") ContextTier contextTier,
        /** True when contextTier is a managed-policy default. Omitted for user-authored and legacy values. */
        @JsonProperty("contextTierManaged") Boolean contextTierManaged,
        /** Auto routing preference selected at session creation time */
        @JsonProperty("autoTier") AutoTier autoTier,
        /** True when autoTier is a managed-policy default. Omitted for user-authored and legacy values. */
        @JsonProperty("autoTierManaged") Boolean autoTierManaged,
        /** Session limits configured at session creation time, if any */
        @JsonProperty("sessionLimits") SessionLimitsConfig sessionLimits,
        /** Working directory and git context at session start */
        @JsonProperty("context") WorkingDirectoryContext context,
        /** Per-session GitHub MCP override persisted for cold resume */
        @JsonProperty("githubMcpToolConfig") GitHubMcpToolConfig gitHubMcpToolConfig,
        /** Whether the session was already in use by another client at start time */
        @JsonProperty("alreadyInUse") Boolean alreadyInUse,
        /** Whether this session supports remote steering via GitHub */
        @JsonProperty("remoteSteerable") Boolean remoteSteerable,
        /** When set, identifies a parent session whose context this session continues — e.g., a detached headless rem-agent run launched on the parent's interactive shutdown. Telemetry from this session is reported under the parent's session_id. */
        @JsonProperty("detachedFromSpawningParentSessionId") String detachedFromSpawningParentSessionId,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Creates a record with the components it had before later optional fields were added.
         * @deprecated Use named construction via {@link #builder()}.
         * @param sessionId Unique identifier for the session
         * @param version Schema version number for the session event format
         * @param producer Identifier of the software producing the events (e.g., "copilot-agent")
         * @param copilotVersion Version string of the Copilot application
         * @param startTime ISO 8601 timestamp when the session was created
         * @param selectedModel Model selected at session creation time, if any
         * @param reasoningEffort Reasoning effort level used for model calls, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max")
         * @param reasoningEffortModel Model that owns effort embedded in an authored model selection. Omitted for independent reasoning-effort overrides and legacy events.
         * @param reasoningSummary Reasoning summary mode used for model calls, if applicable (e.g. "none", "concise", "detailed")
         * @param verbosity Output verbosity level used for model calls, if applicable (e.g. "low", "medium", "high")
         * @param contextTier Context tier selected at session creation time for models with tiered context pricing; null when no tier is selected (e.g., non-tiered model)
         * @param autoTier Auto routing preference selected at session creation time
         * @param sessionLimits Session limits configured at session creation time, if any
         * @param context Working directory and git context at session start
         * @param gitHubMcpToolConfig Per-session GitHub MCP override persisted for cold resume
         * @param alreadyInUse Whether the session was already in use by another client at start time
         * @param remoteSteerable Whether this session supports remote steering via GitHub
         * @param detachedFromSpawningParentSessionId When set, identifies a parent session whose context this session continues — e.g., a detached headless rem-agent run launched on the parent's interactive shutdown. Telemetry from this session is reported under the parent's session_id.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionStartEventData(
            String sessionId,
            Long version,
            String producer,
            String copilotVersion,
            OffsetDateTime startTime,
            String selectedModel,
            String reasoningEffort,
            String reasoningEffortModel,
            ReasoningSummary reasoningSummary,
            Verbosity verbosity,
            ContextTier contextTier,
            AutoTier autoTier,
            SessionLimitsConfig sessionLimits,
            WorkingDirectoryContext context,
            GitHubMcpToolConfig gitHubMcpToolConfig,
            Boolean alreadyInUse,
            Boolean remoteSteerable,
            String detachedFromSpawningParentSessionId
        ) {
            this(sessionId, version, producer, copilotVersion, startTime, selectedModel, null, reasoningEffort, reasoningEffortModel, null, reasoningSummary, verbosity, contextTier, null, autoTier, null, sessionLimits, context, gitHubMcpToolConfig, alreadyInUse, remoteSteerable, detachedFromSpawningParentSessionId);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionStartEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionStartEventData(String sessionId, Long version, String producer, String copilotVersion, OffsetDateTime startTime, String selectedModel, String providerId, String reasoningEffort, String reasoningEffortModel, Boolean reasoningEffortManaged, ReasoningSummary reasoningSummary, Verbosity verbosity, ContextTier contextTier, Boolean contextTierManaged, AutoTier autoTier, Boolean autoTierManaged, SessionLimitsConfig sessionLimits, WorkingDirectoryContext context, GitHubMcpToolConfig gitHubMcpToolConfig, Boolean alreadyInUse, Boolean remoteSteerable, String detachedFromSpawningParentSessionId) {
            this(sessionId, version, producer, copilotVersion, startTime, selectedModel, providerId, reasoningEffort, reasoningEffortModel, reasoningEffortManaged, reasoningSummary, verbosity, contextTier, contextTierManaged, autoTier, autoTierManaged, sessionLimits, context, gitHubMcpToolConfig, alreadyInUse, remoteSteerable, detachedFromSpawningParentSessionId, java.util.Set.of());
        }

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
            private String sessionId;
            private Long version;
            private String producer;
            private String copilotVersion;
            private OffsetDateTime startTime;
            private String selectedModel;
            private String providerId;
            private String reasoningEffort;
            private String reasoningEffortModel;
            private Boolean reasoningEffortManaged;
            private ReasoningSummary reasoningSummary;
            private Verbosity verbosity;
            private ContextTier contextTier;
            private boolean $presence12;
            private Boolean contextTierManaged;
            private AutoTier autoTier;
            private Boolean autoTierManaged;
            private SessionLimitsConfig sessionLimits;
            private WorkingDirectoryContext context;
            private GitHubMcpToolConfig gitHubMcpToolConfig;
            private Boolean alreadyInUse;
            private Boolean remoteSteerable;
            private String detachedFromSpawningParentSessionId;

            /**
             * Sets the {@code sessionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder sessionId(String value) {
                this.sessionId = value;
                return this;
            }

            /**
             * Sets the {@code version} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder version(Long value) {
                this.version = value;
                return this;
            }

            /**
             * Sets the {@code producer} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder producer(String value) {
                this.producer = value;
                return this;
            }

            /**
             * Sets the {@code copilotVersion} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder copilotVersion(String value) {
                this.copilotVersion = value;
                return this;
            }

            /**
             * Sets the {@code startTime} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder startTime(OffsetDateTime value) {
                this.startTime = value;
                return this;
            }

            /**
             * Sets the {@code selectedModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder selectedModel(String value) {
                this.selectedModel = value;
                return this;
            }

            /**
             * Sets the {@code providerId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder providerId(String value) {
                this.providerId = value;
                return this;
            }

            /**
             * Sets the {@code reasoningEffort} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningEffort(String value) {
                this.reasoningEffort = value;
                return this;
            }

            /**
             * Sets the {@code reasoningEffortModel} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningEffortModel(String value) {
                this.reasoningEffortModel = value;
                return this;
            }

            /**
             * Sets the {@code reasoningEffortManaged} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningEffortManaged(Boolean value) {
                this.reasoningEffortManaged = value;
                return this;
            }

            /**
             * Sets the {@code reasoningSummary} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reasoningSummary(ReasoningSummary value) {
                this.reasoningSummary = value;
                return this;
            }

            /**
             * Sets the {@code verbosity} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder verbosity(Verbosity value) {
                this.verbosity = value;
                return this;
            }

            /**
             * Sets the {@code contextTier} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder contextTier(ContextTier value) {
                this.contextTier = value;
                this.$presence12 = true;
                return this;
            }

            /**
             * Sets the {@code contextTierManaged} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder contextTierManaged(Boolean value) {
                this.contextTierManaged = value;
                return this;
            }

            /**
             * Sets the {@code autoTier} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder autoTier(AutoTier value) {
                this.autoTier = value;
                return this;
            }

            /**
             * Sets the {@code autoTierManaged} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder autoTierManaged(Boolean value) {
                this.autoTierManaged = value;
                return this;
            }

            /**
             * Sets the {@code sessionLimits} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder sessionLimits(SessionLimitsConfig value) {
                this.sessionLimits = value;
                return this;
            }

            /**
             * Sets the {@code context} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder context(WorkingDirectoryContext value) {
                this.context = value;
                return this;
            }

            /**
             * Sets the {@code githubMcpToolConfig} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder gitHubMcpToolConfig(GitHubMcpToolConfig value) {
                this.gitHubMcpToolConfig = value;
                return this;
            }

            /**
             * Sets the {@code alreadyInUse} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder alreadyInUse(Boolean value) {
                this.alreadyInUse = value;
                return this;
            }

            /**
             * Sets the {@code remoteSteerable} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder remoteSteerable(Boolean value) {
                this.remoteSteerable = value;
                return this;
            }

            /**
             * Sets the {@code detachedFromSpawningParentSessionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder detachedFromSpawningParentSessionId(String value) {
                this.detachedFromSpawningParentSessionId = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionStartEventData build() {
                if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
                if (version == null) throw new IllegalStateException("Missing required property: version");
                if (producer == null) throw new IllegalStateException("Missing required property: producer");
                if (copilotVersion == null) throw new IllegalStateException("Missing required property: copilotVersion");
                if (startTime == null) throw new IllegalStateException("Missing required property: startTime");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence12 && contextTier == null) $explicitNulls.add("contextTier");
                return new SessionStartEventData(sessionId, version, producer, copilotVersion, startTime, selectedModel, providerId, reasoningEffort, reasoningEffortModel, reasoningEffortManaged, reasoningSummary, verbosity, contextTier, contextTierManaged, autoTier, autoTierManaged, sessionLimits, context, gitHubMcpToolConfig, alreadyInUse, remoteSteerable, detachedFromSpawningParentSessionId, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("contextTier") && contextTier == null) values.put("contextTier", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionStartEventData> {
            @Override
            public SessionStartEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(SessionStartEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("contextTier") && tree.get("contextTier").isNull()) $explicitNulls.add("contextTier");
                return new SessionStartEventData(readProperty(context, tree, "sessionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "version", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "producer", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "copilotVersion", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "startTime", new com.fasterxml.jackson.core.type.TypeReference<OffsetDateTime>() {}), readProperty(context, tree, "selectedModel", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "providerId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningEffort", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningEffortModel", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningEffortManaged", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "reasoningSummary", new com.fasterxml.jackson.core.type.TypeReference<ReasoningSummary>() {}), readProperty(context, tree, "verbosity", new com.fasterxml.jackson.core.type.TypeReference<Verbosity>() {}), readProperty(context, tree, "contextTier", new com.fasterxml.jackson.core.type.TypeReference<ContextTier>() {}), readProperty(context, tree, "contextTierManaged", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "autoTier", new com.fasterxml.jackson.core.type.TypeReference<AutoTier>() {}), readProperty(context, tree, "autoTierManaged", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "sessionLimits", new com.fasterxml.jackson.core.type.TypeReference<SessionLimitsConfig>() {}), readProperty(context, tree, "context", new com.fasterxml.jackson.core.type.TypeReference<WorkingDirectoryContext>() {}), readProperty(context, tree, "githubMcpToolConfig", new com.fasterxml.jackson.core.type.TypeReference<GitHubMcpToolConfig>() {}), readProperty(context, tree, "alreadyInUse", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "remoteSteerable", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "detachedFromSpawningParentSessionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
            }

            private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                    com.fasterxml.jackson.databind.JsonNode tree, String name,
                    com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
                if (!tree.has(name)) return null;
                return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
            }
        }
    }
}
