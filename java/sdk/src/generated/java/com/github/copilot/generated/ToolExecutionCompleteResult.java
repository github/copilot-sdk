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
 * Tool execution result on success
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ToolExecutionCompleteResult.$NullableDeserializer.class)
public record ToolExecutionCompleteResult(
    /** Concise tool result text sent to the LLM for chat completion, potentially truncated for token efficiency */
    @JsonProperty("content") String content,
    /** Detailed tool result for UI/timeline display, preserving complete content such as diffs for most tools. Successful skill invocations intentionally use the concise model-facing content here; the authoritative skill body is carried by the corresponding skill invocation event. Falls back to content when absent. */
    @JsonProperty("detailedContent") String detailedContent,
    /** Structured content blocks (text, images, audio, resources) returned by the tool in their native format */
    @JsonProperty("contents") List<Object> contents,
    /** Model-facing binary results (base64 inline or size-omitted markers) sent to the LLM for this tool call */
    @CopilotExperimental
    @JsonProperty("binaryResultsForLlm") List<Object> binaryResultsForLlm,
    /** MCP Apps UI resource content for rendering in a sandboxed iframe */
    @JsonProperty("uiResource") ToolExecutionCompleteUIResource uiResource,
    /** Structured content (arbitrary JSON) returned verbatim by the MCP tool */
    @JsonProperty("structuredContent") Object structuredContent,
    /** MCP `CallToolResult._meta` returned verbatim by an MCP Apps tool, for delivery to the app UI only; never sent to the model. Experimental. */
    @CopilotExperimental
    @JsonProperty("mcpResultMeta") Map<String, Object> mcpResultMeta,
    /** Provider-neutral source material this tool makes available to the model as citable content. Persisted so it survives session resume. Experimental. */
    @CopilotExperimental
    @JsonProperty("citableSources") List<CitableSource> citableSources,
    /** FIDES IFC label projected from tool ingress metadata (MCP `CallToolResult._meta` or synthesized built-in ingress labels) — persisted as `{ ifc: ... }` (only the `ifc` key, not the whole `_meta`). Persisted so the FIDES IFC label survives session resume: the engine rehydrates accumulated taint by replaying these on load. Populated for ingress sources when FIDES IFC is on. Experimental. */
    @CopilotExperimental
    @JsonProperty("mcpMeta") Object mcpMeta,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ToolExecutionCompleteResult { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ToolExecutionCompleteResult(String content, String detailedContent, List<Object> contents, List<Object> binaryResultsForLlm, ToolExecutionCompleteUIResource uiResource, Object structuredContent, Map<String, Object> mcpResultMeta, List<CitableSource> citableSources, Object mcpMeta) {
        this(content, detailedContent, contents, binaryResultsForLlm, uiResource, structuredContent, mcpResultMeta, citableSources, mcpMeta, java.util.Set.of());
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
        private String content;
        private String detailedContent;
        private List<Object> contents;
        private List<Object> binaryResultsForLlm;
        private ToolExecutionCompleteUIResource uiResource;
        private Object structuredContent;
        private boolean $presence5;
        private Map<String, Object> mcpResultMeta;
        private List<CitableSource> citableSources;
        private Object mcpMeta;
        private boolean $presence8;

        /**
         * Sets the {@code content} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder content(String value) {
            this.content = value;
            return this;
        }

        /**
         * Sets the {@code detailedContent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder detailedContent(String value) {
            this.detailedContent = value;
            return this;
        }

        /**
         * Sets the {@code contents} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder contents(List<Object> value) {
            this.contents = value;
            return this;
        }

        /**
         * Sets the {@code binaryResultsForLlm} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder binaryResultsForLlm(List<Object> value) {
            this.binaryResultsForLlm = value;
            return this;
        }

        /**
         * Sets the {@code uiResource} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder uiResource(ToolExecutionCompleteUIResource value) {
            this.uiResource = value;
            return this;
        }

        /**
         * Sets the {@code structuredContent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder structuredContent(Object value) {
            this.structuredContent = value;
            this.$presence5 = true;
            return this;
        }

        /**
         * Sets the {@code mcpResultMeta} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder mcpResultMeta(Map<String, Object> value) {
            this.mcpResultMeta = value;
            return this;
        }

        /**
         * Sets the {@code citableSources} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder citableSources(List<CitableSource> value) {
            this.citableSources = value;
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
            this.$presence8 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ToolExecutionCompleteResult build() {
            if (content == null) throw new IllegalStateException("Missing required property: content");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence5 && structuredContent == null) $explicitNulls.add("structuredContent");
            if ($presence8 && mcpMeta == null) $explicitNulls.add("mcpMeta");
            return new ToolExecutionCompleteResult(content, detailedContent, contents, binaryResultsForLlm, uiResource, structuredContent, mcpResultMeta, citableSources, mcpMeta, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("structuredContent") && structuredContent == null) values.put("structuredContent", null);
        if ($explicitNulls.contains("mcpMeta") && mcpMeta == null) values.put("mcpMeta", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<ToolExecutionCompleteResult> {
        @Override
        public ToolExecutionCompleteResult deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(ToolExecutionCompleteResult.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("structuredContent") && tree.get("structuredContent").isNull()) $explicitNulls.add("structuredContent");
            if (tree.has("mcpMeta") && tree.get("mcpMeta").isNull()) $explicitNulls.add("mcpMeta");
            return new ToolExecutionCompleteResult(readProperty(context, tree, "content", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "detailedContent", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "contents", new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {}), readProperty(context, tree, "binaryResultsForLlm", new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {}), readProperty(context, tree, "uiResource", new com.fasterxml.jackson.core.type.TypeReference<ToolExecutionCompleteUIResource>() {}), readProperty(context, tree, "structuredContent", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "mcpResultMeta", new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}), readProperty(context, tree, "citableSources", new com.fasterxml.jackson.core.type.TypeReference<List<CitableSource>>() {}), readProperty(context, tree, "mcpMeta", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param content Concise tool result text sent to the LLM for chat completion, potentially truncated for token efficiency
     * @param detailedContent Detailed tool result for UI/timeline display, preserving complete content such as diffs for most tools. Successful skill invocations intentionally use the concise model-facing content here; the authoritative skill body is carried by the corresponding skill invocation event. Falls back to content when absent.
     * @param contents Structured content blocks (text, images, audio, resources) returned by the tool in their native format
     * @param binaryResultsForLlm Model-facing binary results (base64 inline or size-omitted markers) sent to the LLM for this tool call
     * @param uiResource MCP Apps UI resource content for rendering in a sandboxed iframe
     * @param structuredContent Structured content (arbitrary JSON) returned verbatim by the MCP tool
     * @param citableSources Provider-neutral source material this tool makes available to the model as citable content. Persisted so it survives session resume. Experimental.
     * @param mcpMeta FIDES IFC label projected from tool ingress metadata (MCP `CallToolResult._meta` or synthesized built-in ingress labels) — persisted as `{ ifc: ... }` (only the `ifc` key, not the whole `_meta`). Persisted so the FIDES IFC label survives session resume: the engine rehydrates accumulated taint by replaying these on load. Populated for ingress sources when FIDES IFC is on. Experimental.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ToolExecutionCompleteResult(
        String content,
        String detailedContent,
        List<Object> contents,
        List<Object> binaryResultsForLlm,
        ToolExecutionCompleteUIResource uiResource,
        Object structuredContent,
        List<CitableSource> citableSources,
        Object mcpMeta
    ) {
        this(content, detailedContent, contents, binaryResultsForLlm, uiResource, structuredContent, null, citableSources, mcpMeta);
    }
}
