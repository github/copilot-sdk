/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Tolerant decoder for optional worker diagnostics, not a causal association
 * validator.
 */
public final class WorkerCausalityDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {
    private static final Logger LOG = Logger.getLogger(WorkerCausalityDeserializer.class.getName());
    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}");
    private final JavaType target;

    /** Creates the annotation's contextual decoder. */
    public WorkerCausalityDeserializer() {
        this(null);
    }

    private WorkerCausalityDeserializer(JavaType target) {
        this.target = target;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
        return new WorkerCausalityDeserializer(property.getType());
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode value = parser.readValueAsTree();
        if (small(value, new int[]{4096}, 0) && supported(value)) {
            String field = "{\"workerCausality\":" + value + "}";
            if (field.getBytes(StandardCharsets.UTF_8).length <= 4096) {
                try {
                    return context.readTreeAsValue(value, target);
                } catch (IOException | IllegalArgumentException ignored) {
                    // Optional diagnostics must not reject the product event.
                }
            }
        }
        LOG.warning("Ignoring invalid, unsupported or oversized workerCausality metadata");
        return null;
    }

    private static boolean supported(JsonNode value) {
        if (!fields(value, "version", "observationProvenance", "sources", "captureComplete")
                || !value.path("version").isIntegralNumber() || !value.path("version").canConvertToInt()
                || value.path("version").asInt() != 1
                || !(value.path("observationProvenance").asText().equals("native")
                        || value.path("observationProvenance").asText().equals("ahp_coordinator"))
                || !value.path("captureComplete").isBoolean() || !value.path("sources").isArray()
                || value.path("sources").size() > 32) {
            return false;
        }
        for (JsonNode source : value.path("sources")) {
            JsonNode input = source.path("input");
            if (!fields(source, "input", "admissions", "captureComplete", "completion", "notification",
                    "admittedDuring") || !fields(input, "queueItemId", "agentId", "sender", "senderBridges")
                    || !source.path("captureComplete").isBoolean() || !source.path("admissions").isArray()
                    || source.path("admissions").size() > 32 || !input.isObject()
                    || (value.path("captureComplete").asBoolean() && !source.path("captureComplete").asBoolean())
                    || !uuid(input.path("queueItemId")) || !text(input.path("agentId"))
                    || !optionalReference(input, "sender", "tool.execution_start")
                    || !optionalReference(source, "completion", "subagent.completed")
                    || !optionalReference(source, "admittedDuring", "assistant.turn_start")) {
                return false;
            }
            if (input.has("senderBridges")) {
                JsonNode edges = input.path("senderBridges");
                if (!input.has("sender") || !edges.isArray() || edges.size() > 32) {
                    return false;
                }
                for (JsonNode edge : edges) {
                    if (!fields(edge, "source", "reported") || !reference(edge.path("source"), "tool.execution_start")
                            || !reference(edge.path("reported"), "tool.execution_start")) {
                        return false;
                    }
                }
            }
            for (JsonNode admission : source.path("admissions")) {
                String kind = admission.path("kind").asText();
                if (!fields(admission, "kind", "messageId", "event", "ahpTurnId")
                        || !(kind.equals("queued_input") || kind.equals("system_continuation"))
                        || !text(admission.path("messageId"))
                        || (admission.has("ahpTurnId") && !uuid(admission.path("ahpTurnId")))
                        || (admission.has("event") && (!reference(admission.path("event"), "user.message")
                                || !admission.path("event").path("agentId").equals(input.path("agentId"))))) {
                    return false;
                }
            }
            if (source.has("notification")) {
                JsonNode notification = source.path("notification");
                String mode = notification.path("mode").asText();
                if (!fields(notification, "deliveryId", "event", "mode") || !uuid(notification.path("deliveryId"))
                        || !(mode.equals("queued") || mode.equals("immediate"))
                        || !optionalReference(notification, "event", "system.notification")) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean fields(JsonNode value, String... allowed) {
        if (!value.isObject()) {
            return false;
        }
        var names = value.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            boolean found = false;
            for (String candidate : allowed) {
                if (candidate.equals(name)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static boolean text(JsonNode value) {
        return value.isTextual() && !value.textValue().isEmpty()
                && value.textValue().getBytes(StandardCharsets.UTF_8).length <= 256
                && value.textValue().codePoints().noneMatch(Character::isISOControl);
    }

    private static boolean uuid(JsonNode value) {
        return value.isTextual() && UUID.matcher(value.textValue()).matches();
    }

    private static boolean optionalReference(JsonNode value, String field, String eventType) {
        return !value.has(field) || reference(value.path(field), eventType);
    }

    private static boolean reference(JsonNode value, String eventType) {
        return fields(value, "sessionId", "eventId", "agentId", "eventType", "provenance")
                && text(value.path("sessionId")) && uuid(value.path("eventId"))
                && (!value.has("agentId") || text(value.path("agentId")))
                && value.path("eventType").asText().equals(eventType)
                && (value.path("provenance").asText().equals("native")
                        || value.path("provenance").asText().equals("ahp_coordinator"));
    }

    private static boolean small(JsonNode value, int[] remaining, int depth) {
        if (--remaining[0] < 0 || depth > 32) {
            return false;
        }
        if (value.isTextual()) {
            remaining[0] -= value.textValue().length();
        } else if (value.isObject()) {
            var fields = value.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                remaining[0] -= field.getKey().length();
                if (!small(field.getValue(), remaining, depth + 1)) {
                    return false;
                }
            }
        } else if (value.isArray()) {
            for (JsonNode item : value) {
                if (!small(item, remaining, depth + 1)) {
                    return false;
                }
            }
        }
        return remaining[0] >= 0;
    }
}
