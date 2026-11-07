/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Session event "permission.requested". Permission request notification requiring client approval with request details
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class PermissionRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "permission.requested"; }

    @JsonProperty("data")
    private PermissionRequestedEventData data;

    public PermissionRequestedEventData getData() { return data; }
    public void setData(PermissionRequestedEventData data) { this.data = data; }

    /** Data payload for {@link PermissionRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = PermissionRequestedEventData.$NullableDeserializer.class)
    public record PermissionRequestedEventData(
        /** Unique identifier for this permission request; used to respond via session.respondToPermission() */
        @JsonProperty("requestId") String requestId,
        /** Details of the permission being requested */
        @JsonProperty("permissionRequest") Object permissionRequest,
        /** Derived user-facing permission prompt details for UI consumers */
        @JsonProperty("promptRequest") Object promptRequest,
        /** Canonical runtime-owned Assisted Permissions assessment. New runtimes emit this for every permission request; absence means the event predates this field. Consumers must use `permission.completed` for the final permission result. */
        @CopilotExperimental
        @JsonProperty("assistedApprovalAssessment") AssistedApprovalAssessment assistedApprovalAssessment,
        /** Permission mode captured when evaluation began. Absent on historical events. */
        @JsonProperty("permissionMode") PermissionMode permissionMode,
        /** Agent mode captured from the owning turn when permission evaluation began. */
        @JsonProperty("agentMode") SessionMode agentMode,
        /** Execution session that owns permission recovery for this request. Absent on historical events. */
        @JsonProperty("ownerSessionId") String ownerSessionId,
        /** Delegated agent that owns permission recovery when the owner session multiplexes remote agents. Absent otherwise. */
        @JsonProperty("ownerAgentId") String ownerAgentId,
        /** Neutral risk metadata supplied by the tool host. Consumers may display this value but must not use it to bypass the permission decision. */
        @JsonProperty("riskAssessment") Object riskAssessment,
        /** When true, this permission was already resolved by a permissionRequest hook and requires no client action */
        @JsonProperty("resolvedByHook") Boolean resolvedByHook,
        /** Permission-recovery episode that authorized this request to surface for interactive attention */
        @JsonProperty("recoveryEpisodeId") String recoveryEpisodeId,
        /** Experimental HydraFusion attribution of the staged phase whose tool call raised this request. Absent outside a staged phase. */
        @CopilotExperimental
        @JsonProperty("fusion") FusionAttribution fusion,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param requestId Unique identifier for this permission request; used to respond via session.respondToPermission()
         * @param permissionRequest Details of the permission being requested
         * @param promptRequest Derived user-facing permission prompt details for UI consumers
         * @param permissionMode Permission mode captured when evaluation began. Absent on historical events.
         * @param agentMode Agent mode captured from the owning turn when permission evaluation began.
         * @param ownerSessionId Execution session that owns permission recovery for this request. Absent on historical events.
         * @param ownerAgentId Delegated agent that owns permission recovery when the owner session multiplexes remote agents. Absent otherwise.
         * @param riskAssessment Neutral risk metadata supplied by the tool host. Consumers may display this value but must not use it to bypass the permission decision.
         * @param resolvedByHook When true, this permission was already resolved by a permissionRequest hook and requires no client action
         * @param recoveryEpisodeId Permission-recovery episode that authorized this request to surface for interactive attention
         * @param fusion Experimental HydraFusion attribution of the staged phase whose tool call raised this request. Absent outside a staged phase.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public PermissionRequestedEventData(
            String requestId,
            Object permissionRequest,
            Object promptRequest,
            PermissionMode permissionMode,
            SessionMode agentMode,
            String ownerSessionId,
            String ownerAgentId,
            Object riskAssessment,
            Boolean resolvedByHook,
            String recoveryEpisodeId,
            FusionAttribution fusion
        ) {
            this(requestId, permissionRequest, promptRequest, null, permissionMode, agentMode, ownerSessionId, ownerAgentId, riskAssessment, resolvedByHook, recoveryEpisodeId, fusion);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public PermissionRequestedEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public PermissionRequestedEventData(String requestId, Object permissionRequest, Object promptRequest, AssistedApprovalAssessment assistedApprovalAssessment, PermissionMode permissionMode, SessionMode agentMode, String ownerSessionId, String ownerAgentId, Object riskAssessment, Boolean resolvedByHook, String recoveryEpisodeId, FusionAttribution fusion) {
            this(requestId, permissionRequest, promptRequest, assistedApprovalAssessment, permissionMode, agentMode, ownerSessionId, ownerAgentId, riskAssessment, resolvedByHook, recoveryEpisodeId, fusion, java.util.Set.of());
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
            private String requestId;
            private Object permissionRequest;
            private Object promptRequest;
            private AssistedApprovalAssessment assistedApprovalAssessment;
            private PermissionMode permissionMode;
            private SessionMode agentMode;
            private String ownerSessionId;
            private String ownerAgentId;
            private Object riskAssessment;
            private boolean $presence8;
            private Boolean resolvedByHook;
            private String recoveryEpisodeId;
            private FusionAttribution fusion;

            /**
             * Sets the {@code requestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestId(String value) {
                this.requestId = value;
                return this;
            }

            /**
             * Sets the {@code permissionRequest} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder permissionRequest(Object value) {
                this.permissionRequest = value;
                return this;
            }

            /**
             * Sets the {@code promptRequest} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder promptRequest(Object value) {
                this.promptRequest = value;
                return this;
            }

            /**
             * Sets the {@code assistedApprovalAssessment} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder assistedApprovalAssessment(AssistedApprovalAssessment value) {
                this.assistedApprovalAssessment = value;
                return this;
            }

            /**
             * Sets the {@code permissionMode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder permissionMode(PermissionMode value) {
                this.permissionMode = value;
                return this;
            }

            /**
             * Sets the {@code agentMode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder agentMode(SessionMode value) {
                this.agentMode = value;
                return this;
            }

            /**
             * Sets the {@code ownerSessionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder ownerSessionId(String value) {
                this.ownerSessionId = value;
                return this;
            }

            /**
             * Sets the {@code ownerAgentId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder ownerAgentId(String value) {
                this.ownerAgentId = value;
                return this;
            }

            /**
             * Sets the {@code riskAssessment} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder riskAssessment(Object value) {
                this.riskAssessment = value;
                this.$presence8 = true;
                return this;
            }

            /**
             * Sets the {@code resolvedByHook} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder resolvedByHook(Boolean value) {
                this.resolvedByHook = value;
                return this;
            }

            /**
             * Sets the {@code recoveryEpisodeId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder recoveryEpisodeId(String value) {
                this.recoveryEpisodeId = value;
                return this;
            }

            /**
             * Sets the {@code fusion} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder fusion(FusionAttribution value) {
                this.fusion = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public PermissionRequestedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (permissionRequest == null) throw new IllegalStateException("Missing required property: permissionRequest");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence8 && riskAssessment == null) $explicitNulls.add("riskAssessment");
                return new PermissionRequestedEventData(requestId, permissionRequest, promptRequest, assistedApprovalAssessment, permissionMode, agentMode, ownerSessionId, ownerAgentId, riskAssessment, resolvedByHook, recoveryEpisodeId, fusion, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("riskAssessment") && riskAssessment == null) values.put("riskAssessment", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<PermissionRequestedEventData> {
            @Override
            public PermissionRequestedEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(PermissionRequestedEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("riskAssessment") && tree.get("riskAssessment").isNull()) $explicitNulls.add("riskAssessment");
                return new PermissionRequestedEventData(readProperty(context, tree, "requestId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "permissionRequest", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "promptRequest", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "assistedApprovalAssessment", new com.fasterxml.jackson.core.type.TypeReference<AssistedApprovalAssessment>() {}), readProperty(context, tree, "permissionMode", new com.fasterxml.jackson.core.type.TypeReference<PermissionMode>() {}), readProperty(context, tree, "agentMode", new com.fasterxml.jackson.core.type.TypeReference<SessionMode>() {}), readProperty(context, tree, "ownerSessionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "ownerAgentId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "riskAssessment", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "resolvedByHook", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "recoveryEpisodeId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "fusion", new com.fasterxml.jackson.core.type.TypeReference<FusionAttribution>() {}), $explicitNulls);
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
