/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

/**
 * Input for a session-end hook.
 * <p>
 * This hook is invoked when a session ends, allowing you to perform cleanup or
 * logging.
 *
 * @param sessionId
 *            the runtime session ID of the session that triggered the hook
 * @param timestamp
 *            the timestamp in milliseconds since epoch when the session ended
 * @param cwd
 *            the current working directory
 * @param reason
 *            the reason: "complete", "error", "abort", "timeout", or
 *            "user_exit"
 * @param finalMessage
 *            the final message, or {@code null}
 * @param error
 *            the error message, or {@code null}; the CLI sends an error object,
 *            whose {@code message} this is
 * @since 1.0.7
 * @param agentId
 *            optional subagent identity, absent for root hooks and older
 *            runtimes
 * @param agentType
 *            optional subagent identity, absent for root hooks and older
 *            runtimes
 * @param parentAgentId
 *            optional subagent identity, absent for root hooks and older
 *            runtimes
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionEndHookInput(@JsonProperty("sessionId") String sessionId,
        @JsonProperty("timestamp") long timestamp, @JsonProperty("cwd") String cwd,
        @JsonProperty("reason") String reason, @JsonProperty("finalMessage") String finalMessage,
        @JsonProperty("error") @JsonDeserialize(using = HookErrorTextDeserializer.class) String error,
        @JsonProperty("agentId") String agentId, @JsonProperty("agentType") String agentType,
        @JsonProperty("parentAgentId") String parentAgentId) {
    public SessionEndHookInput(String sessionId, long timestamp, String cwd, String reason, String finalMessage,
            String error) {
        this(sessionId, timestamp, cwd, reason, finalMessage, error, null, null, null);
    }

    /**
     * Reads the CLI's error object ({@code {"message"}}) as its message, and an
     * object without a string message as its JSON. A string passes through.
     */
    private static final class HookErrorTextDeserializer extends JsonDeserializer<String> {

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.currentToken() == JsonToken.START_OBJECT) {
                JsonNode error = context.readTree(parser);
                JsonNode message = error.get("message");
                return message != null && message.isTextual() ? message.textValue() : error.toString();
            }
            return StringDeserializer.instance.deserialize(parser, context);
        }
    }
}
