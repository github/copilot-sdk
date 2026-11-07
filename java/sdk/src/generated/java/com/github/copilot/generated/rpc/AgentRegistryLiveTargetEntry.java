/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Full registry entry for the spawned child. Lets the controller call `handleLiveTargetSelected(entry)` directly without re-reading the registry (avoids a TOCTOU window).
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = AgentRegistryLiveTargetEntry.$NullableDeserializer.class)
public record AgentRegistryLiveTargetEntry(
    /** Registry entry schema version (1 = ui-server, 2 = managed-server) */
    @JsonProperty("schemaVersion") Long schemaVersion,
    /** Process kind tag for the registry entry */
    @JsonProperty("kind") AgentRegistryLiveTargetEntryKind kind,
    /** Operating-system pid of the process owning this entry */
    @JsonProperty("pid") Long pid,
    /** Bind host for the entry's JSON-RPC server */
    @JsonProperty("host") String host,
    /** TCP port the entry's JSON-RPC server is listening on */
    @JsonProperty("port") Long port,
    /** Session ID of the foreground session for this entry */
    @JsonProperty("sessionId") String sessionId,
    /** Friendly session name (when set) */
    @JsonProperty("sessionName") String sessionName,
    /** Working directory of the session (when known) */
    @JsonProperty("cwd") String cwd,
    /** Git branch of the session (when known) */
    @JsonProperty("branch") String branch,
    /** Model identifier currently selected for the session */
    @JsonProperty("model") String model,
    /** Coarse lifecycle status of the foreground session */
    @JsonProperty("status") AgentRegistryLiveTargetEntryStatus status,
    /** Kind of attention required when status === "attention". Meaningful only when status === "attention". */
    @JsonProperty("attentionKind") AgentRegistryLiveTargetEntryAttentionKind attentionKind,
    /** Monotonic per-publisher revision counter incremented on every status update. Lets watchers detect transient flips. */
    @JsonProperty("statusRevision") Long statusRevision,
    /** How the most recent turn ended (clean vs aborted). Lets the renderer distinguish done from done_cancelled. */
    @JsonProperty("lastTerminalEvent") AgentRegistryLiveTargetEntryLastTerminalEvent lastTerminalEvent,
    /** ISO 8601 timestamp captured at registration */
    @JsonProperty("startedAt") String startedAt,
    /** Copilot CLI version that wrote the entry */
    @JsonProperty("copilotVersion") String copilotVersion,
    /** Wall-clock milliseconds since the watcher last observed this entry (heartbeat freshness) */
    @JsonProperty("lastSeenMs") Long lastSeenMs,
    /** Connection token (null when the target is unauthenticated) */
    @JsonProperty("token") String token,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AgentRegistryLiveTargetEntry { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AgentRegistryLiveTargetEntry(Long schemaVersion, AgentRegistryLiveTargetEntryKind kind, Long pid, String host, Long port, String sessionId, String sessionName, String cwd, String branch, String model, AgentRegistryLiveTargetEntryStatus status, AgentRegistryLiveTargetEntryAttentionKind attentionKind, Long statusRevision, AgentRegistryLiveTargetEntryLastTerminalEvent lastTerminalEvent, String startedAt, String copilotVersion, Long lastSeenMs, String token) {
        this(schemaVersion, kind, pid, host, port, sessionId, sessionName, cwd, branch, model, status, attentionKind, statusRevision, lastTerminalEvent, startedAt, copilotVersion, lastSeenMs, token, java.util.Set.of());
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
        private Long schemaVersion;
        private AgentRegistryLiveTargetEntryKind kind;
        private Long pid;
        private String host;
        private Long port;
        private String sessionId;
        private String sessionName;
        private String cwd;
        private String branch;
        private String model;
        private AgentRegistryLiveTargetEntryStatus status;
        private AgentRegistryLiveTargetEntryAttentionKind attentionKind;
        private Long statusRevision;
        private AgentRegistryLiveTargetEntryLastTerminalEvent lastTerminalEvent;
        private String startedAt;
        private String copilotVersion;
        private Long lastSeenMs;
        private String token;
        private boolean $presence17;

        /**
         * Sets the {@code schemaVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder schemaVersion(Long value) {
            this.schemaVersion = value;
            return this;
        }

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(AgentRegistryLiveTargetEntryKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code pid} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pid(Long value) {
            this.pid = value;
            return this;
        }

        /**
         * Sets the {@code host} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder host(String value) {
            this.host = value;
            return this;
        }

        /**
         * Sets the {@code port} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder port(Long value) {
            this.port = value;
            return this;
        }

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
         * Sets the {@code sessionName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionName(String value) {
            this.sessionName = value;
            return this;
        }

        /**
         * Sets the {@code cwd} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cwd(String value) {
            this.cwd = value;
            return this;
        }

        /**
         * Sets the {@code branch} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder branch(String value) {
            this.branch = value;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(AgentRegistryLiveTargetEntryStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code attentionKind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder attentionKind(AgentRegistryLiveTargetEntryAttentionKind value) {
            this.attentionKind = value;
            return this;
        }

        /**
         * Sets the {@code statusRevision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder statusRevision(Long value) {
            this.statusRevision = value;
            return this;
        }

        /**
         * Sets the {@code lastTerminalEvent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder lastTerminalEvent(AgentRegistryLiveTargetEntryLastTerminalEvent value) {
            this.lastTerminalEvent = value;
            return this;
        }

        /**
         * Sets the {@code startedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder startedAt(String value) {
            this.startedAt = value;
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
         * Sets the {@code lastSeenMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder lastSeenMs(Long value) {
            this.lastSeenMs = value;
            return this;
        }

        /**
         * Sets the {@code token} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder token(String value) {
            this.token = value;
            this.$presence17 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AgentRegistryLiveTargetEntry build() {
            if (schemaVersion == null) throw new IllegalStateException("Missing required property: schemaVersion");
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (pid == null) throw new IllegalStateException("Missing required property: pid");
            if (host == null) throw new IllegalStateException("Missing required property: host");
            if (port == null) throw new IllegalStateException("Missing required property: port");
            if (startedAt == null) throw new IllegalStateException("Missing required property: startedAt");
            if (copilotVersion == null) throw new IllegalStateException("Missing required property: copilotVersion");
            if (lastSeenMs == null) throw new IllegalStateException("Missing required property: lastSeenMs");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence17 && token == null) $explicitNulls.add("token");
            return new AgentRegistryLiveTargetEntry(schemaVersion, kind, pid, host, port, sessionId, sessionName, cwd, branch, model, status, attentionKind, statusRevision, lastTerminalEvent, startedAt, copilotVersion, lastSeenMs, token, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("token") && token == null) values.put("token", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<AgentRegistryLiveTargetEntry> {
        @Override
        public AgentRegistryLiveTargetEntry deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(AgentRegistryLiveTargetEntry.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("token") && tree.get("token").isNull()) $explicitNulls.add("token");
            return new AgentRegistryLiveTargetEntry(readProperty(context, tree, "schemaVersion", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "kind", new com.fasterxml.jackson.core.type.TypeReference<AgentRegistryLiveTargetEntryKind>() {}), readProperty(context, tree, "pid", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "host", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "port", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "sessionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "sessionName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "cwd", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "branch", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "model", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "status", new com.fasterxml.jackson.core.type.TypeReference<AgentRegistryLiveTargetEntryStatus>() {}), readProperty(context, tree, "attentionKind", new com.fasterxml.jackson.core.type.TypeReference<AgentRegistryLiveTargetEntryAttentionKind>() {}), readProperty(context, tree, "statusRevision", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "lastTerminalEvent", new com.fasterxml.jackson.core.type.TypeReference<AgentRegistryLiveTargetEntryLastTerminalEvent>() {}), readProperty(context, tree, "startedAt", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "copilotVersion", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "lastSeenMs", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "token", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
