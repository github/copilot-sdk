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
 * Session event "session.resume". Session resume metadata including current context and event count
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionResumeEvent extends SessionEvent {

    @Override
    public String getType() { return "session.resume"; }

    @JsonProperty("data")
    private SessionResumeEventData data;

    public SessionResumeEventData getData() { return data; }
    public void setData(SessionResumeEventData data) { this.data = data; }

    /** Data payload for {@link SessionResumeEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SessionResumeEventData.$NullableDeserializer.class)
    public record SessionResumeEventData(
        /** ISO 8601 timestamp when the session was resumed */
        @JsonProperty("resumeTime") OffsetDateTime resumeTime,
        /** Total number of persisted events in the session at the time of resume */
        @JsonProperty("eventCount") Long eventCount,
        /** On-disk byte size of the session's persisted events.jsonl file at resume time; omitted when the file does not exist or cannot be stat'd */
        @JsonProperty("eventsFileSizeBytes") Long eventsFileSizeBytes,
        /** Model currently selected at resume time */
        @JsonProperty("selectedModel") String selectedModel,
        /** Provider of selectedModel at resume time, when explicitly selected. */
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
        /** Context tier currently selected at resume time; null when no tier is active */
        @JsonProperty("contextTier") ContextTier contextTier,
        /** True when contextTier is a managed-policy default. Omitted for user-authored and legacy values. */
        @JsonProperty("contextTierManaged") Boolean contextTierManaged,
        /** Auto routing preference active at resume time */
        @JsonProperty("autoTier") AutoTier autoTier,
        /** True when autoTier is a managed-policy default. Omitted for user-authored and legacy values. */
        @JsonProperty("autoTierManaged") Boolean autoTierManaged,
        /** Session limits currently configured at resume time; null when no limits are active */
        @JsonProperty("sessionLimits") SessionLimitsConfig sessionLimits,
        /** Updated working directory and git context at resume time */
        @JsonProperty("context") WorkingDirectoryContext context,
        /** Whether the session was already in use by another client at resume time */
        @JsonProperty("alreadyInUse") Boolean alreadyInUse,
        /** True when this resume passively joined a session that already had live work running in the runtime - an agent turn, a native queue run, a queued resume continuation, or an in-flight send (for example, an extension joining a session another client was actively driving). False (or omitted) when the session had no live work or when the resume explicitly abandoned pending work, including cold resumes and suspended sessions that remain resident in memory. */
        @JsonProperty("sessionWasActive") Boolean sessionWasActive,
        /** Whether this session supports remote steering via GitHub */
        @JsonProperty("remoteSteerable") Boolean remoteSteerable,
        /** When true, tool calls and permission requests left in flight by the previous session lifetime remain pending after resume and the agentic loop awaits their results. User sends are queued behind the pending work until all such requests reach a terminal state. When false or omitted, pending work is normally marked as interrupted unless the resume passively joined live work owned by another client; sessionWasActive distinguishes that case. */
        @JsonProperty("continuePendingWork") Boolean continuePendingWork,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Creates a record with the components it had before later optional fields were added.
         * @deprecated Use named construction via {@link #builder()}.
         * @param resumeTime ISO 8601 timestamp when the session was resumed
         * @param eventCount Total number of persisted events in the session at the time of resume
         * @param eventsFileSizeBytes On-disk byte size of the session's persisted events.jsonl file at resume time; omitted when the file does not exist or cannot be stat'd
         * @param selectedModel Model currently selected at resume time
         * @param reasoningEffort Reasoning effort level used for model calls, if applicable (e.g. "none", "low", "medium", "high", "xhigh", "max")
         * @param reasoningEffortModel Model that owns effort embedded in an authored model selection. Omitted for independent reasoning-effort overrides and legacy events.
         * @param reasoningSummary Reasoning summary mode used for model calls, if applicable (e.g. "none", "concise", "detailed")
         * @param verbosity Output verbosity level used for model calls, if applicable (e.g. "low", "medium", "high")
         * @param contextTier Context tier currently selected at resume time; null when no tier is active
         * @param autoTier Auto routing preference active at resume time
         * @param sessionLimits Session limits currently configured at resume time; null when no limits are active
         * @param context Updated working directory and git context at resume time
         * @param alreadyInUse Whether the session was already in use by another client at resume time
         * @param sessionWasActive True when this resume passively joined a session that already had live work running in the runtime - an agent turn, a native queue run, a queued resume continuation, or an in-flight send (for example, an extension joining a session another client was actively driving). False (or omitted) when the session had no live work or when the resume explicitly abandoned pending work, including cold resumes and suspended sessions that remain resident in memory.
         * @param remoteSteerable Whether this session supports remote steering via GitHub
         * @param continuePendingWork When true, tool calls and permission requests left in flight by the previous session lifetime remain pending after resume and the agentic loop awaits their results. User sends are queued behind the pending work until all such requests reach a terminal state. When false or omitted, pending work is normally marked as interrupted unless the resume passively joined live work owned by another client; sessionWasActive distinguishes that case.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionResumeEventData(
            OffsetDateTime resumeTime,
            Long eventCount,
            Long eventsFileSizeBytes,
            String selectedModel,
            String reasoningEffort,
            String reasoningEffortModel,
            ReasoningSummary reasoningSummary,
            Verbosity verbosity,
            ContextTier contextTier,
            AutoTier autoTier,
            SessionLimitsConfig sessionLimits,
            WorkingDirectoryContext context,
            Boolean alreadyInUse,
            Boolean sessionWasActive,
            Boolean remoteSteerable,
            Boolean continuePendingWork
        ) {
            this(resumeTime, eventCount, eventsFileSizeBytes, selectedModel, null, reasoningEffort, reasoningEffortModel, null, reasoningSummary, verbosity, contextTier, null, autoTier, null, sessionLimits, context, alreadyInUse, sessionWasActive, remoteSteerable, continuePendingWork);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionResumeEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionResumeEventData(OffsetDateTime resumeTime, Long eventCount, Long eventsFileSizeBytes, String selectedModel, String providerId, String reasoningEffort, String reasoningEffortModel, Boolean reasoningEffortManaged, ReasoningSummary reasoningSummary, Verbosity verbosity, ContextTier contextTier, Boolean contextTierManaged, AutoTier autoTier, Boolean autoTierManaged, SessionLimitsConfig sessionLimits, WorkingDirectoryContext context, Boolean alreadyInUse, Boolean sessionWasActive, Boolean remoteSteerable, Boolean continuePendingWork) {
            this(resumeTime, eventCount, eventsFileSizeBytes, selectedModel, providerId, reasoningEffort, reasoningEffortModel, reasoningEffortManaged, reasoningSummary, verbosity, contextTier, contextTierManaged, autoTier, autoTierManaged, sessionLimits, context, alreadyInUse, sessionWasActive, remoteSteerable, continuePendingWork, java.util.Set.of());
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
            private OffsetDateTime resumeTime;
            private Long eventCount;
            private Long eventsFileSizeBytes;
            private String selectedModel;
            private String providerId;
            private String reasoningEffort;
            private String reasoningEffortModel;
            private Boolean reasoningEffortManaged;
            private ReasoningSummary reasoningSummary;
            private Verbosity verbosity;
            private ContextTier contextTier;
            private boolean $presence10;
            private Boolean contextTierManaged;
            private AutoTier autoTier;
            private Boolean autoTierManaged;
            private SessionLimitsConfig sessionLimits;
            private boolean $presence14;
            private WorkingDirectoryContext context;
            private Boolean alreadyInUse;
            private Boolean sessionWasActive;
            private Boolean remoteSteerable;
            private Boolean continuePendingWork;

            /**
             * Sets the {@code resumeTime} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder resumeTime(OffsetDateTime value) {
                this.resumeTime = value;
                return this;
            }

            /**
             * Sets the {@code eventCount} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder eventCount(Long value) {
                this.eventCount = value;
                return this;
            }

            /**
             * Sets the {@code eventsFileSizeBytes} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder eventsFileSizeBytes(Long value) {
                this.eventsFileSizeBytes = value;
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
                this.$presence10 = true;
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
                this.$presence14 = true;
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
             * Sets the {@code sessionWasActive} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder sessionWasActive(Boolean value) {
                this.sessionWasActive = value;
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
             * Sets the {@code continuePendingWork} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder continuePendingWork(Boolean value) {
                this.continuePendingWork = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionResumeEventData build() {
                if (resumeTime == null) throw new IllegalStateException("Missing required property: resumeTime");
                if (eventCount == null) throw new IllegalStateException("Missing required property: eventCount");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence10 && contextTier == null) $explicitNulls.add("contextTier");
                if ($presence14 && sessionLimits == null) $explicitNulls.add("sessionLimits");
                return new SessionResumeEventData(resumeTime, eventCount, eventsFileSizeBytes, selectedModel, providerId, reasoningEffort, reasoningEffortModel, reasoningEffortManaged, reasoningSummary, verbosity, contextTier, contextTierManaged, autoTier, autoTierManaged, sessionLimits, context, alreadyInUse, sessionWasActive, remoteSteerable, continuePendingWork, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("contextTier") && contextTier == null) values.put("contextTier", null);
            if ($explicitNulls.contains("sessionLimits") && sessionLimits == null) values.put("sessionLimits", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<SessionResumeEventData> {
            @Override
            public SessionResumeEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(SessionResumeEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("contextTier") && tree.get("contextTier").isNull()) $explicitNulls.add("contextTier");
                if (tree.has("sessionLimits") && tree.get("sessionLimits").isNull()) $explicitNulls.add("sessionLimits");
                return new SessionResumeEventData(readProperty(context, tree, "resumeTime", new com.fasterxml.jackson.core.type.TypeReference<OffsetDateTime>() {}), readProperty(context, tree, "eventCount", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "eventsFileSizeBytes", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "selectedModel", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "providerId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningEffort", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningEffortModel", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "reasoningEffortManaged", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "reasoningSummary", new com.fasterxml.jackson.core.type.TypeReference<ReasoningSummary>() {}), readProperty(context, tree, "verbosity", new com.fasterxml.jackson.core.type.TypeReference<Verbosity>() {}), readProperty(context, tree, "contextTier", new com.fasterxml.jackson.core.type.TypeReference<ContextTier>() {}), readProperty(context, tree, "contextTierManaged", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "autoTier", new com.fasterxml.jackson.core.type.TypeReference<AutoTier>() {}), readProperty(context, tree, "autoTierManaged", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "sessionLimits", new com.fasterxml.jackson.core.type.TypeReference<SessionLimitsConfig>() {}), readProperty(context, tree, "context", new com.fasterxml.jackson.core.type.TypeReference<WorkingDirectoryContext>() {}), readProperty(context, tree, "alreadyInUse", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "sessionWasActive", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "remoteSteerable", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "continuePendingWork", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), $explicitNulls);
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
