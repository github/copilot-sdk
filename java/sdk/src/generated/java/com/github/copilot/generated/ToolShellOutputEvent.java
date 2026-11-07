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
 * Session event "tool.shell_output". Live, append-only shell output. Not persisted or replayed to late subscribers. Text is decoded and redacted per chunk; chunks need not contain complete lines.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ToolShellOutputEvent extends SessionEvent {

    @Override
    public String getType() { return "tool.shell_output"; }

    @JsonProperty("data")
    private ToolShellOutputEventData data;

    public ToolShellOutputEventData getData() { return data; }
    public void setData(ToolShellOutputEventData data) { this.data = data; }

    /** Data payload for {@link ToolShellOutputEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolShellOutputEventData(
        /** Tool call ID that owns this shell output */
        @JsonProperty("toolCallId") String toolCallId,
        /** Output source. Omission means stdout. Terminal output has no separate stdout/stderr attribution. */
        @JsonProperty("stream") ToolShellOutputStream stream,
        /** New output to append, without synthetic shell-result markers or stream-switch separators */
        @JsonProperty("text") String text,
        /** Zero-based publication sequence across all output streams for this tool call. Not a byte offset or an OS write-order guarantee. */
        @JsonProperty("sequence") Long sequence,
        /** Experimental HydraFusion attribution of the staged phase whose tool call produced this output. Absent outside a staged phase. */
        @CopilotExperimental
        @JsonProperty("fusion") FusionAttribution fusion
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ToolShellOutputEventData {}

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
            private ToolShellOutputStream stream;
            private String text;
            private Long sequence;
            private FusionAttribution fusion;

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
             * Sets the {@code stream} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder stream(ToolShellOutputStream value) {
                this.stream = value;
                return this;
            }

            /**
             * Sets the {@code text} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder text(String value) {
                this.text = value;
                return this;
            }

            /**
             * Sets the {@code sequence} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder sequence(Long value) {
                this.sequence = value;
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
            public ToolShellOutputEventData build() {
                if (toolCallId == null) throw new IllegalStateException("Missing required property: toolCallId");
                if (text == null) throw new IllegalStateException("Missing required property: text");
                if (sequence == null) throw new IllegalStateException("Missing required property: sequence");
                return new ToolShellOutputEventData(toolCallId, stream, text, sequence, fusion);
            }
        }
    }
}
