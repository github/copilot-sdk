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
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "tool.execution_complete". Tool execution completion results including success status, detailed output, and error information
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolExecutionCompleteEvent extends SessionEvent {

    @Override
    public String getType() { return "tool.execution_complete"; }

    @JsonProperty("data")
    private ToolExecutionCompleteEventData data;

    public ToolExecutionCompleteEventData getData() { return data; }
    public void setData(ToolExecutionCompleteEventData data) { this.data = data; }

    /** Data payload for {@link ToolExecutionCompleteEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ToolExecutionCompleteEventData.$NullableDeserializer.class)
    public record ToolExecutionCompleteEventData(
        /** IDs of accepted assistant messages whose output this tool result consumed, referring to assistant.message.data.messageId across the agent tree. Captured from the returned snapshot, including partial agent reads. An empty list means no model-produced dependency; absence means provenance is unavailable. */
        @JsonProperty("causalDependencies") List<String> causalDependencies,
        /** Unique identifier for the completed tool call */
        @JsonProperty("toolCallId") String toolCallId,
        /** Whether the tool execution completed successfully */
        @JsonProperty("success") Boolean success,
        /** Model identifier that generated this tool call */
        @JsonProperty("model") String model,
        /** Provider-reported usage of a separately metered runtime-owned tool operation, independent of the conversation model. Present on success or failure when usage is known. Experimental. */
        @CopilotExperimental
        @JsonProperty("toolUsage") ToolExecutionUsage toolUsage,
        /** FIDES IFC label projected from tool ingress metadata (MCP `CallToolResult._meta` or synthesized built-in ingress labels). Persisted as `{ ifc: ... }` so the label survives session resume, including model-visible failure results. Experimental. */
        @CopilotExperimental
        @JsonProperty("mcpMeta") Object mcpMeta,
        /** CAPI interaction ID for correlating this tool execution with upstream telemetry */
        @JsonProperty("interactionId") String interactionId,
        /** Per-request treatment/eligibility signal returned by the Copilot API in the `X-GitHub-Copilot-Request-TE` response header for the associated model call; `false` when the header was absent or unparseable. */
        @JsonProperty("rte") Boolean rte,
        /** Whether this tool call was explicitly requested by the user rather than the assistant */
        @JsonProperty("isUserRequested") Boolean isUserRequested,
        /** Tool execution result on success */
        @JsonProperty("result") ToolExecutionCompleteResult result,
        /** Error details when the tool execution failed */
        @JsonProperty("error") ToolExecutionCompleteError error,
        /** Tool-specific telemetry data (e.g., CodeQL check counts, grep match counts) */
        @JsonProperty("toolTelemetry") Map<String, Object> toolTelemetry,
        /** Identifier for the agent loop turn this tool was invoked in, matching the corresponding assistant.turn_start event */
        @JsonProperty("turnId") String turnId,
        /** Tool definition metadata, present for MCP tools with MCP Apps support */
        @JsonProperty("toolDescription") ToolExecutionCompleteToolDescription toolDescription,
        /** Whether this tool execution ran inside a sandbox container */
        @JsonProperty("sandboxed") Boolean sandboxed,
        /** Experimental shell completion facts captured before the persisted result contents are stripped. */
        @CopilotExperimental
        @JsonProperty("shellExecution") ToolExecutionCompleteShellExecution shellExecution,
        /** Experimental. File mutations actually committed by a built-in file editing tool, in execution order. Present on successful edits and on partial failures when earlier mutations were committed. Paths are absolute in the session filesystem namespace. */
        @CopilotExperimental
        @JsonProperty("fileEdits") List<ToolExecutionCompleteFileEdit> fileEdits,
        /** Tool call ID of the parent tool invocation when this event originates from a sub-agent */
        @Deprecated
        @JsonProperty("parentToolCallId") String parentToolCallId,
        /** Experimental HydraFusion attribution for this tool completion. */
        @CopilotExperimental
        @JsonProperty("fusion") FusionAttribution fusion,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param toolCallId Unique identifier for the completed tool call
         * @param success Whether the tool execution completed successfully
         * @param model Model identifier that generated this tool call
         * @param toolUsage Provider-reported usage of a separately metered runtime-owned tool operation, independent of the conversation model. Present on success or failure when usage is known. Experimental.
         * @param mcpMeta FIDES IFC label projected from tool ingress metadata (MCP `CallToolResult._meta` or synthesized built-in ingress labels). Persisted as `{ ifc: ... }` so the label survives session resume, including model-visible failure results. Experimental.
         * @param interactionId CAPI interaction ID for correlating this tool execution with upstream telemetry
         * @param rte Per-request treatment/eligibility signal returned by the Copilot API in the `X-GitHub-Copilot-Request-TE` response header for the associated model call; `false` when the header was absent or unparseable.
         * @param isUserRequested Whether this tool call was explicitly requested by the user rather than the assistant
         * @param result Tool execution result on success
         * @param error Error details when the tool execution failed
         * @param toolTelemetry Tool-specific telemetry data (e.g., CodeQL check counts, grep match counts)
         * @param turnId Identifier for the agent loop turn this tool was invoked in, matching the corresponding assistant.turn_start event
         * @param toolDescription Tool definition metadata, present for MCP tools with MCP Apps support
         * @param sandboxed Whether this tool execution ran inside a sandbox container
         * @param shellExecution Experimental shell completion facts captured before the persisted result contents are stripped.
         * @param fileEdits Experimental. File mutations actually committed by a built-in file editing tool, in execution order. Present on successful edits and on partial failures when earlier mutations were committed. Paths are absolute in the session filesystem namespace.
         * @param parentToolCallId Tool call ID of the parent tool invocation when this event originates from a sub-agent
         * @param fusion Experimental HydraFusion attribution for this tool completion.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ToolExecutionCompleteEventData(
            String toolCallId,
            Boolean success,
            String model,
            ToolExecutionUsage toolUsage,
            Object mcpMeta,
            String interactionId,
            Boolean rte,
            Boolean isUserRequested,
            ToolExecutionCompleteResult result,
            ToolExecutionCompleteError error,
            Map<String, Object> toolTelemetry,
            String turnId,
            ToolExecutionCompleteToolDescription toolDescription,
            Boolean sandboxed,
            ToolExecutionCompleteShellExecution shellExecution,
            List<ToolExecutionCompleteFileEdit> fileEdits,
            String parentToolCallId,
            FusionAttribution fusion
        ) {
            this(null, toolCallId, success, model, toolUsage, mcpMeta, interactionId, rte, isUserRequested, result, error, toolTelemetry, turnId, toolDescription, sandboxed, shellExecution, fileEdits, parentToolCallId, fusion);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ToolExecutionCompleteEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ToolExecutionCompleteEventData(List<String> causalDependencies, String toolCallId, Boolean success, String model, ToolExecutionUsage toolUsage, Object mcpMeta, String interactionId, Boolean rte, Boolean isUserRequested, ToolExecutionCompleteResult result, ToolExecutionCompleteError error, Map<String, Object> toolTelemetry, String turnId, ToolExecutionCompleteToolDescription toolDescription, Boolean sandboxed, ToolExecutionCompleteShellExecution shellExecution, List<ToolExecutionCompleteFileEdit> fileEdits, String parentToolCallId, FusionAttribution fusion) {
            this(causalDependencies, toolCallId, success, model, toolUsage, mcpMeta, interactionId, rte, isUserRequested, result, error, toolTelemetry, turnId, toolDescription, sandboxed, shellExecution, fileEdits, parentToolCallId, fusion, java.util.Set.of());
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
            private List<String> causalDependencies;
            private String toolCallId;
            private Boolean success;
            private String model;
            private ToolExecutionUsage toolUsage;
            private Object mcpMeta;
            private boolean $presence5;
            private String interactionId;
            private Boolean rte;
            private Boolean isUserRequested;
            private ToolExecutionCompleteResult result;
            private ToolExecutionCompleteError error;
            private Map<String, Object> toolTelemetry;
            private String turnId;
            private ToolExecutionCompleteToolDescription toolDescription;
            private Boolean sandboxed;
            private ToolExecutionCompleteShellExecution shellExecution;
            private List<ToolExecutionCompleteFileEdit> fileEdits;
            private String parentToolCallId;
            private FusionAttribution fusion;

            /**
             * Sets the {@code causalDependencies} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder causalDependencies(List<String> value) {
                this.causalDependencies = value;
                return this;
            }

            /**
             * Sets the {@code toolCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolCallId(String value) {
                this.toolCallId = value;
                return this;
            }

            /**
             * Sets the {@code success} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder success(Boolean value) {
                this.success = value;
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
             * Sets the {@code toolUsage} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder toolUsage(ToolExecutionUsage value) {
                this.toolUsage = value;
                return this;
            }

            /**
             * Sets the {@code mcpMeta} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder mcpMeta(Object value) {
                this.mcpMeta = value;
                this.$presence5 = true;
                return this;
            }

            /**
             * Sets the {@code interactionId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder interactionId(String value) {
                this.interactionId = value;
                return this;
            }

            /**
             * Sets the {@code rte} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder rte(Boolean value) {
                this.rte = value;
                return this;
            }

            /**
             * Sets the {@code isUserRequested} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder isUserRequested(Boolean value) {
                this.isUserRequested = value;
                return this;
            }

            /**
             * Sets the {@code result} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder result(ToolExecutionCompleteResult value) {
                this.result = value;
                return this;
            }

            /**
             * Sets the {@code error} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder error(ToolExecutionCompleteError value) {
                this.error = value;
                return this;
            }

            /**
             * Sets the {@code toolTelemetry} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolTelemetry(Map<String, Object> value) {
                this.toolTelemetry = value;
                return this;
            }

            /**
             * Sets the {@code turnId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder turnId(String value) {
                this.turnId = value;
                return this;
            }

            /**
             * Sets the {@code toolDescription} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolDescription(ToolExecutionCompleteToolDescription value) {
                this.toolDescription = value;
                return this;
            }

            /**
             * Sets the {@code sandboxed} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder sandboxed(Boolean value) {
                this.sandboxed = value;
                return this;
            }

            /**
             * Sets the {@code shellExecution} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder shellExecution(ToolExecutionCompleteShellExecution value) {
                this.shellExecution = value;
                return this;
            }

            /**
             * Sets the {@code fileEdits} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            @com.github.copilot.CopilotExperimental
            public Builder fileEdits(List<ToolExecutionCompleteFileEdit> value) {
                this.fileEdits = value;
                return this;
            }

            /**
             * Sets the {@code parentToolCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder parentToolCallId(String value) {
                this.parentToolCallId = value;
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
            public ToolExecutionCompleteEventData build() {
                if (toolCallId == null) throw new IllegalStateException("Missing required property: toolCallId");
                if (success == null) throw new IllegalStateException("Missing required property: success");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence5 && mcpMeta == null) $explicitNulls.add("mcpMeta");
                return new ToolExecutionCompleteEventData(causalDependencies, toolCallId, success, model, toolUsage, mcpMeta, interactionId, rte, isUserRequested, result, error, toolTelemetry, turnId, toolDescription, sandboxed, shellExecution, fileEdits, parentToolCallId, fusion, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("mcpMeta") && mcpMeta == null) values.put("mcpMeta", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ToolExecutionCompleteEventData> {
            @Override
            public ToolExecutionCompleteEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(ToolExecutionCompleteEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("mcpMeta") && tree.get("mcpMeta").isNull()) $explicitNulls.add("mcpMeta");
                return new ToolExecutionCompleteEventData(readProperty(context, tree, "causalDependencies", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "toolCallId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "success", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "model", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "toolUsage", new com.fasterxml.jackson.core.type.TypeReference<ToolExecutionUsage>() {}), readProperty(context, tree, "mcpMeta", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "interactionId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "rte", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "isUserRequested", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "result", new com.fasterxml.jackson.core.type.TypeReference<ToolExecutionCompleteResult>() {}), readProperty(context, tree, "error", new com.fasterxml.jackson.core.type.TypeReference<ToolExecutionCompleteError>() {}), readProperty(context, tree, "toolTelemetry", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}), readProperty(context, tree, "turnId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "toolDescription", new com.fasterxml.jackson.core.type.TypeReference<ToolExecutionCompleteToolDescription>() {}), readProperty(context, tree, "sandboxed", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "shellExecution", new com.fasterxml.jackson.core.type.TypeReference<ToolExecutionCompleteShellExecution>() {}), readProperty(context, tree, "fileEdits", new com.fasterxml.jackson.core.type.TypeReference<List<ToolExecutionCompleteFileEdit>>() {}), readProperty(context, tree, "parentToolCallId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "fusion", new com.fasterxml.jackson.core.type.TypeReference<FusionAttribution>() {}), $explicitNulls);
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
