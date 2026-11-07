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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Expanded canonical result returned by a session tool.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ToolResultExpanded.$NullableDeserializer.class)
public record ToolResultExpanded(
    /** Text result returned to the model. */
    @JsonProperty("textResultForLlm") String textResultForLlm,
    /** Execution outcome classification. */
    @JsonProperty("resultType") ToolResultType resultType,
    /** Base64-encoded binary results returned to the model. */
    @JsonProperty("binaryResultsForLlm") List<ExternalToolTextResultForLlmBinaryResultsForLlm> binaryResultsForLlm,
    /** Detailed log content available for session display. */
    @JsonProperty("sessionLog") String sessionLog,
    /** Error message for an unsuccessful execution. */
    @JsonProperty("error") String error,
    /** Tool-specific telemetry payload. */
    @JsonProperty("toolTelemetry") Object toolTelemetry,
    /** Whether large-output post-processing should be skipped. */
    @JsonProperty("skipLargeOutputProcessing") Boolean skipLargeOutputProcessing,
    /** Messages to inject after the tool result. */
    @JsonProperty("newMessages") List<ToolResultNewMessage> newMessages,
    /** Structured content blocks returned to the model. */
    @JsonProperty("contents") List<Object> contents,
    /** Deferred tool names made available by this result. */
    @JsonProperty("toolReferences") List<String> toolReferences,
    /** Sources returned by the tool that the model may cite. */
    @JsonProperty("citableSources") List<Object> citableSources,
    /** Skill invocation metadata produced by the tool. */
    @JsonProperty("skillInvocation") Object skillInvocation,
    /** Whether post-tool-use failure hooks have already processed this result. */
    @JsonProperty("postToolUseFailureHooksProcessed") Boolean postToolUseFailureHooksProcessed,
    /** Optional UI resource produced by the tool. */
    @JsonProperty("uiResource") Object uiResource,
    /** Metadata propagated with the tool result, including information-flow labels. */
    @JsonProperty("mcpMeta") Map<String, Object> mcpMeta,
    /** Structured result content in addition to the model-facing text. */
    @JsonProperty("structuredContent") Object structuredContent,
    /** Completion-review decision produced by the task-completion tool. */
    @JsonProperty("taskCompletionDecision") TaskCompletionDecision taskCompletionDecision,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ToolResultExpanded { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ToolResultExpanded(String textResultForLlm, ToolResultType resultType, List<ExternalToolTextResultForLlmBinaryResultsForLlm> binaryResultsForLlm, String sessionLog, String error, Object toolTelemetry, Boolean skipLargeOutputProcessing, List<ToolResultNewMessage> newMessages, List<Object> contents, List<String> toolReferences, List<Object> citableSources, Object skillInvocation, Boolean postToolUseFailureHooksProcessed, Object uiResource, Map<String, Object> mcpMeta, Object structuredContent, TaskCompletionDecision taskCompletionDecision) {
        this(textResultForLlm, resultType, binaryResultsForLlm, sessionLog, error, toolTelemetry, skipLargeOutputProcessing, newMessages, contents, toolReferences, citableSources, skillInvocation, postToolUseFailureHooksProcessed, uiResource, mcpMeta, structuredContent, taskCompletionDecision, java.util.Set.of());
    }

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
        private String textResultForLlm;
        private ToolResultType resultType;
        private List<ExternalToolTextResultForLlmBinaryResultsForLlm> binaryResultsForLlm;
        private String sessionLog;
        private String error;
        private Object toolTelemetry;
        private boolean $presence5;
        private Boolean skipLargeOutputProcessing;
        private List<ToolResultNewMessage> newMessages;
        private List<Object> contents;
        private List<String> toolReferences;
        private List<Object> citableSources;
        private Object skillInvocation;
        private boolean $presence11;
        private Boolean postToolUseFailureHooksProcessed;
        private Object uiResource;
        private boolean $presence13;
        private Map<String, Object> mcpMeta;
        private Object structuredContent;
        private boolean $presence15;
        private TaskCompletionDecision taskCompletionDecision;

        /**
         * Sets the {@code textResultForLlm} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder textResultForLlm(String value) {
            this.textResultForLlm = value;
            return this;
        }

        /**
         * Sets the {@code resultType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder resultType(ToolResultType value) {
            this.resultType = value;
            return this;
        }

        /**
         * Sets the {@code binaryResultsForLlm} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder binaryResultsForLlm(List<ExternalToolTextResultForLlmBinaryResultsForLlm> value) {
            this.binaryResultsForLlm = value;
            return this;
        }

        /**
         * Sets the {@code sessionLog} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionLog(String value) {
            this.sessionLog = value;
            return this;
        }

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder error(String value) {
            this.error = value;
            return this;
        }

        /**
         * Sets the {@code toolTelemetry} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder toolTelemetry(Object value) {
            this.toolTelemetry = value;
            this.$presence5 = true;
            return this;
        }

        /**
         * Sets the {@code skipLargeOutputProcessing} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder skipLargeOutputProcessing(Boolean value) {
            this.skipLargeOutputProcessing = value;
            return this;
        }

        /**
         * Sets the {@code newMessages} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder newMessages(List<ToolResultNewMessage> value) {
            this.newMessages = value;
            return this;
        }

        /**
         * Sets the {@code contents} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder contents(List<Object> value) {
            this.contents = value;
            return this;
        }

        /**
         * Sets the {@code toolReferences} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder toolReferences(List<String> value) {
            this.toolReferences = value;
            return this;
        }

        /**
         * Sets the {@code citableSources} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder citableSources(List<Object> value) {
            this.citableSources = value;
            return this;
        }

        /**
         * Sets the {@code skillInvocation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder skillInvocation(Object value) {
            this.skillInvocation = value;
            this.$presence11 = true;
            return this;
        }

        /**
         * Sets the {@code postToolUseFailureHooksProcessed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder postToolUseFailureHooksProcessed(Boolean value) {
            this.postToolUseFailureHooksProcessed = value;
            return this;
        }

        /**
         * Sets the {@code uiResource} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder uiResource(Object value) {
            this.uiResource = value;
            this.$presence13 = true;
            return this;
        }

        /**
         * Sets the {@code mcpMeta} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder mcpMeta(Map<String, Object> value) {
            this.mcpMeta = value;
            return this;
        }

        /**
         * Sets the {@code structuredContent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder structuredContent(Object value) {
            this.structuredContent = value;
            this.$presence15 = true;
            return this;
        }

        /**
         * Sets the {@code taskCompletionDecision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder taskCompletionDecision(TaskCompletionDecision value) {
            this.taskCompletionDecision = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ToolResultExpanded build() {
            if (textResultForLlm == null) throw new IllegalStateException("Missing required property: textResultForLlm");
            if (resultType == null) throw new IllegalStateException("Missing required property: resultType");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence5 && toolTelemetry == null) $explicitNulls.add("toolTelemetry");
            if ($presence11 && skillInvocation == null) $explicitNulls.add("skillInvocation");
            if ($presence13 && uiResource == null) $explicitNulls.add("uiResource");
            if ($presence15 && structuredContent == null) $explicitNulls.add("structuredContent");
            return new ToolResultExpanded(textResultForLlm, resultType, binaryResultsForLlm, sessionLog, error, toolTelemetry, skipLargeOutputProcessing, newMessages, contents, toolReferences, citableSources, skillInvocation, postToolUseFailureHooksProcessed, uiResource, mcpMeta, structuredContent, taskCompletionDecision, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("toolTelemetry") && toolTelemetry == null) values.put("toolTelemetry", null);
        if ($explicitNulls.contains("skillInvocation") && skillInvocation == null) values.put("skillInvocation", null);
        if ($explicitNulls.contains("uiResource") && uiResource == null) values.put("uiResource", null);
        if ($explicitNulls.contains("structuredContent") && structuredContent == null) values.put("structuredContent", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ToolResultExpanded> {
        @Override
        public ToolResultExpanded deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(ToolResultExpanded.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("toolTelemetry") && tree.get("toolTelemetry").isNull()) $explicitNulls.add("toolTelemetry");
            if (tree.has("skillInvocation") && tree.get("skillInvocation").isNull()) $explicitNulls.add("skillInvocation");
            if (tree.has("uiResource") && tree.get("uiResource").isNull()) $explicitNulls.add("uiResource");
            if (tree.has("structuredContent") && tree.get("structuredContent").isNull()) $explicitNulls.add("structuredContent");
            return new ToolResultExpanded(readProperty(context, tree, "textResultForLlm", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "resultType", new com.fasterxml.jackson.core.type.TypeReference<ToolResultType>() {}), readProperty(context, tree, "binaryResultsForLlm", new com.fasterxml.jackson.core.type.TypeReference<List<ExternalToolTextResultForLlmBinaryResultsForLlm>>() {}), readProperty(context, tree, "sessionLog", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "error", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "toolTelemetry", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "skipLargeOutputProcessing", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "newMessages", new com.fasterxml.jackson.core.type.TypeReference<List<ToolResultNewMessage>>() {}), readProperty(context, tree, "contents", new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {}), readProperty(context, tree, "toolReferences", new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}), readProperty(context, tree, "citableSources", new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {}), readProperty(context, tree, "skillInvocation", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "postToolUseFailureHooksProcessed", new com.fasterxml.jackson.core.type.TypeReference<Boolean>() {}), readProperty(context, tree, "uiResource", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "mcpMeta", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}), readProperty(context, tree, "structuredContent", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "taskCompletionDecision", new com.fasterxml.jackson.core.type.TypeReference<TaskCompletionDecision>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
