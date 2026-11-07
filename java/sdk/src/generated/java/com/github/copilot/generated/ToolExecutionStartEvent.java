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
 * Session event "tool.execution_start". Tool execution startup details including MCP server information when applicable
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolExecutionStartEvent extends SessionEvent {

    @Override
    public String getType() { return "tool.execution_start"; }

    @JsonProperty("data")
    private ToolExecutionStartEventData data;

    public ToolExecutionStartEventData getData() { return data; }
    public void setData(ToolExecutionStartEventData data) { this.data = data; }

    /** Data payload for {@link ToolExecutionStartEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ToolExecutionStartEventData.$NullableDeserializer.class)
    public record ToolExecutionStartEventData(
        /** Unique identifier for this tool call */
        @JsonProperty("toolCallId") String toolCallId,
        /** Name of the tool being executed */
        @JsonProperty("toolName") String toolName,
        /** Human-readable display title for the tool, when the selected tool descriptor has a non-empty title. */
        @JsonProperty("toolTitle") String toolTitle,
        /** Whether the selected tool descriptor explicitly classifies this call as read-only. */
        @JsonProperty("readOnly") Boolean readOnly,
        /** Arguments passed to the tool */
        @JsonProperty("arguments") Object arguments,
        /** Shell-tool path hints derived from the command at start time for shell tools (bash/powershell/local_shell). Produced by the same shell-aware extractor as PermissionRequestShell.possiblePaths, so it is present even when the command is auto-approved and no permission request fires. Absent for non-shell tools. */
        @JsonProperty("shellToolInfo") ToolExecutionStartShellToolInfo shellToolInfo,
        /** Model identifier that generated this tool call */
        @JsonProperty("model") String model,
        /** Per-request treatment/eligibility signal returned by the Copilot API in the `X-GitHub-Copilot-Request-TE` response header for the associated model call; `false` when the header was absent or unparseable. */
        @JsonProperty("rte") Boolean rte,
        /** Configured key of the MCP server hosting this tool. Legacy events may contain the display name instead; prefer mcpConfigServerName for their identity when available. */
        @JsonProperty("mcpServerName") String mcpServerName,
        /** Configured MCP server key, retained for compatibility. New events write the same value as mcpServerName; on legacy events this field distinguishes the configured key from the display name in mcpServerName. Unrestricted provenance telemetry hashes this key to join with mcp_server_setup. */
        @JsonProperty("mcpConfigServerName") String mcpConfigServerName,
        /** Original tool name on the MCP server, when the tool is an MCP tool */
        @JsonProperty("mcpToolName") String mcpToolName,
        /** Transport the MCP server hosting this tool is connected over, when the tool is an MCP tool and the server is configured */
        @JsonProperty("mcpTransport") McpServerTransport mcpTransport,
        /** Where the MCP server's configuration came from (`user`, `workspace`, `plugin`, or `builtin`), when the tool is an MCP tool and the server is configured */
        @JsonProperty("mcpConfigSource") McpServerSource mcpConfigSource,
        /** Identifier for the agent loop turn this tool was invoked in, matching the corresponding assistant.turn_start event */
        @JsonProperty("turnId") String turnId,
        /** When true, the tool output should be displayed expanded (verbatim) in the CLI timeline */
        @JsonProperty("displayVerbatim") Boolean displayVerbatim,
        /** Tool definition metadata, present for MCP tools with MCP Apps support */
        @JsonProperty("toolDescription") ToolExecutionStartToolDescription toolDescription,
        /** Tool call ID of the parent tool invocation when this event originates from a sub-agent */
        @Deprecated
        @JsonProperty("parentToolCallId") String parentToolCallId,
        /** Experimental HydraFusion attribution for this tool execution. */
        @CopilotExperimental
        @JsonProperty("fusion") FusionAttribution fusion,
        /** Explicitly configured MCP server display name. */
        @JsonProperty("mcpServerDisplayName") String mcpServerDisplayName,
        @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
    ) {

        /**
         * Creates a record with the components it had before later optional fields were added.
         * @deprecated Use named construction via {@link #builder()}.
         * @param toolCallId Unique identifier for this tool call
         * @param toolName Name of the tool being executed
         * @param toolTitle Human-readable display title for the tool, when the selected tool descriptor has a non-empty title.
         * @param arguments Arguments passed to the tool
         * @param shellToolInfo Shell-tool path hints derived from the command at start time for shell tools (bash/powershell/local_shell). Produced by the same shell-aware extractor as PermissionRequestShell.possiblePaths, so it is present even when the command is auto-approved and no permission request fires. Absent for non-shell tools.
         * @param model Model identifier that generated this tool call
         * @param rte Per-request treatment/eligibility signal returned by the Copilot API in the `X-GitHub-Copilot-Request-TE` response header for the associated model call; `false` when the header was absent or unparseable.
         * @param mcpServerName Configured key of the MCP server hosting this tool. Legacy events may contain the display name instead; prefer mcpConfigServerName for their identity when available.
         * @param mcpConfigServerName Configured MCP server key, retained for compatibility. New events write the same value as mcpServerName; on legacy events this field distinguishes the configured key from the display name in mcpServerName. Unrestricted provenance telemetry hashes this key to join with mcp_server_setup.
         * @param mcpToolName Original tool name on the MCP server, when the tool is an MCP tool
         * @param mcpTransport Transport the MCP server hosting this tool is connected over, when the tool is an MCP tool and the server is configured
         * @param mcpConfigSource Where the MCP server's configuration came from (`user`, `workspace`, `plugin`, or `builtin`), when the tool is an MCP tool and the server is configured
         * @param turnId Identifier for the agent loop turn this tool was invoked in, matching the corresponding assistant.turn_start event
         * @param displayVerbatim When true, the tool output should be displayed expanded (verbatim) in the CLI timeline
         * @param toolDescription Tool definition metadata, present for MCP tools with MCP Apps support
         * @param parentToolCallId Tool call ID of the parent tool invocation when this event originates from a sub-agent
         * @param fusion Experimental HydraFusion attribution for this tool execution.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ToolExecutionStartEventData(
            String toolCallId,
            String toolName,
            String toolTitle,
            Object arguments,
            ToolExecutionStartShellToolInfo shellToolInfo,
            String model,
            Boolean rte,
            String mcpServerName,
            String mcpConfigServerName,
            String mcpToolName,
            McpServerTransport mcpTransport,
            McpServerSource mcpConfigSource,
            String turnId,
            Boolean displayVerbatim,
            ToolExecutionStartToolDescription toolDescription,
            String parentToolCallId,
            FusionAttribution fusion
        ) {
            this(toolCallId, toolName, toolTitle, null, arguments, shellToolInfo, model, rte, mcpServerName, mcpConfigServerName, mcpToolName, mcpTransport, mcpConfigSource, turnId, displayVerbatim, toolDescription, parentToolCallId, fusion, null);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ToolExecutionStartEventData { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

        /**
         * Creates a record with legacy null-as-omission semantics.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ToolExecutionStartEventData(String toolCallId, String toolName, String toolTitle, Boolean readOnly, Object arguments, ToolExecutionStartShellToolInfo shellToolInfo, String model, Boolean rte, String mcpServerName, String mcpConfigServerName, String mcpToolName, McpServerTransport mcpTransport, McpServerSource mcpConfigSource, String turnId, Boolean displayVerbatim, ToolExecutionStartToolDescription toolDescription, String parentToolCallId, FusionAttribution fusion, String mcpServerDisplayName) {
            this(toolCallId, toolName, toolTitle, readOnly, arguments, shellToolInfo, model, rte, mcpServerName, mcpConfigServerName, mcpToolName, mcpTransport, mcpConfigSource, turnId, displayVerbatim, toolDescription, parentToolCallId, fusion, mcpServerDisplayName, java.util.Set.of());
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
            private String toolCallId;
            private String toolName;
            private String toolTitle;
            private Boolean readOnly;
            private Object arguments;
            private boolean $presence4;
            private ToolExecutionStartShellToolInfo shellToolInfo;
            private String model;
            private Boolean rte;
            private String mcpServerName;
            private String mcpConfigServerName;
            private String mcpToolName;
            private McpServerTransport mcpTransport;
            private McpServerSource mcpConfigSource;
            private String turnId;
            private Boolean displayVerbatim;
            private ToolExecutionStartToolDescription toolDescription;
            private String parentToolCallId;
            private FusionAttribution fusion;
            private String mcpServerDisplayName;

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
             * Sets the {@code toolName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolName(String value) {
                this.toolName = value;
                return this;
            }

            /**
             * Sets the {@code toolTitle} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolTitle(String value) {
                this.toolTitle = value;
                return this;
            }

            /**
             * Sets the {@code readOnly} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder readOnly(Boolean value) {
                this.readOnly = value;
                return this;
            }

            /**
             * Sets the {@code arguments} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder arguments(Object value) {
                this.arguments = value;
                this.$presence4 = true;
                return this;
            }

            /**
             * Sets the {@code shellToolInfo} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder shellToolInfo(ToolExecutionStartShellToolInfo value) {
                this.shellToolInfo = value;
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
             * Sets the {@code mcpServerName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mcpServerName(String value) {
                this.mcpServerName = value;
                return this;
            }

            /**
             * Sets the {@code mcpConfigServerName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mcpConfigServerName(String value) {
                this.mcpConfigServerName = value;
                return this;
            }

            /**
             * Sets the {@code mcpToolName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mcpToolName(String value) {
                this.mcpToolName = value;
                return this;
            }

            /**
             * Sets the {@code mcpTransport} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mcpTransport(McpServerTransport value) {
                this.mcpTransport = value;
                return this;
            }

            /**
             * Sets the {@code mcpConfigSource} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mcpConfigSource(McpServerSource value) {
                this.mcpConfigSource = value;
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
             * Sets the {@code displayVerbatim} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder displayVerbatim(Boolean value) {
                this.displayVerbatim = value;
                return this;
            }

            /**
             * Sets the {@code toolDescription} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolDescription(ToolExecutionStartToolDescription value) {
                this.toolDescription = value;
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
             * Sets the {@code mcpServerDisplayName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mcpServerDisplayName(String value) {
                this.mcpServerDisplayName = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public ToolExecutionStartEventData build() {
                if (toolCallId == null) throw new IllegalStateException("Missing required property: toolCallId");
                if (toolName == null) throw new IllegalStateException("Missing required property: toolName");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if ($presence4 && arguments == null) $explicitNulls.add("arguments");
                return new ToolExecutionStartEventData(toolCallId, toolName, toolTitle, readOnly, arguments, shellToolInfo, model, rte, mcpServerName, mcpConfigServerName, mcpToolName, mcpTransport, mcpConfigSource, turnId, displayVerbatim, toolDescription, parentToolCallId, fusion, mcpServerDisplayName, $explicitNulls);
            }
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
        public java.util.Map<String, Object> $nullableProperties() {
            java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
            if ($explicitNulls.contains("arguments") && arguments == null) values.put("arguments", null);
            return values;
        }

        public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ToolExecutionStartEventData> {
            @Override
            public ToolExecutionStartEventData deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
                if (!tree.isObject()) return context.reportInputMismatch(ToolExecutionStartEventData.class, "Expected a JSON object");
                java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
                if (tree.has("arguments") && tree.get("arguments").isNull()) $explicitNulls.add("arguments");
                return new ToolExecutionStartEventData(readProperty(context, tree, "toolCallId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "toolName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "toolTitle", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "readOnly", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "arguments", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "shellToolInfo", new com.fasterxml.jackson.core.type.TypeReference<ToolExecutionStartShellToolInfo>() {}), readProperty(context, tree, "model", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "rte", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "mcpServerName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "mcpConfigServerName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "mcpToolName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "mcpTransport", new com.fasterxml.jackson.core.type.TypeReference<McpServerTransport>() {}), readProperty(context, tree, "mcpConfigSource", new com.fasterxml.jackson.core.type.TypeReference<McpServerSource>() {}), readProperty(context, tree, "turnId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "displayVerbatim", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "toolDescription", new com.fasterxml.jackson.core.type.TypeReference<ToolExecutionStartToolDescription>() {}), readProperty(context, tree, "parentToolCallId", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "fusion", new com.fasterxml.jackson.core.type.TypeReference<FusionAttribution>() {}), readProperty(context, tree, "mcpServerDisplayName", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), $explicitNulls);
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
