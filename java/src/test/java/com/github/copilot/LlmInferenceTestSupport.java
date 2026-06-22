/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.rpc.CopilotClientOptions;

/**
 * Shared synthetic-upstream helpers for the LLM inference callback e2e tests.
 *
 * <p>
 * These tests have no recorded snapshots: the registered callback fabricates
 * well-formed model responses and the runtime routes all of its model-layer
 * HTTP/WebSocket traffic through that callback instead of the CAPI proxy. The
 * helpers centralise the synthetic CAPI shapes (model catalog, policy,
 * {@code /responses} SSE, {@code /chat/completions}) so each test focuses on
 * the behaviour it is exercising.
 * </p>
 */
final class LlmInferenceTestSupport {

    static final String SYNTHETIC_TEXT = "OK from the synthetic stream.";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern STREAM_TRUE = Pattern.compile("\"stream\"\\s*:\\s*true");

    private LlmInferenceTestSupport() {
    }

    /**
     * Builds a client wired to {@code handler} via {@link LlmInferenceConfig}. The
     * shared context client has no inference callback, so each inference test owns
     * an isolated client carrying its own handler. {@code extraEnv} entries
     * (formatted {@code KEY=value}) are added to the spawned runtime's environment,
     * e.g. to flip an ExP flag for the WebSocket transport.
     */
    static CopilotClient newLlmClient(E2ETestContext ctx, LlmInferenceProvider handler, String... extraEnv) {
        Map<String, String> env = new HashMap<>(ctx.getEnvironment());
        for (String entry : extraEnv) {
            int eq = entry.indexOf('=');
            if (eq > 0) {
                env.put(entry.substring(0, eq), entry.substring(eq + 1));
            }
        }
        return ctx.createClient(new CopilotClientOptions().setEnvironment(env)
                .setLlmInference(new LlmInferenceConfig().setHandler(handler)));
    }

    /**
     * Initializes the proxy state and registers a synthetic CAPI user so the
     * runtime can resolve auth for sessions that route their model-layer traffic
     * through the callback instead of the proxy.
     */
    static void setupCapiAuth(E2ETestContext ctx) throws IOException, InterruptedException {
        ctx.initializeProxy();
        ctx.setCopilotUserByToken("fake-token-for-e2e-tests", "e2e-user", "individual_pro", ctx.getProxyUrl(),
                "https://localhost:1/telemetry", "e2e-tracking-id");
    }

    static Map<String, List<String>> headers(String name, String value) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put(name, List.of(value));
        return headers;
    }

    static Map<String, List<String>> emptyHeaders() {
        return new LinkedHashMap<>();
    }

    static String json(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    static boolean wantsStream(String body) {
        return STREAM_TRUE.matcher(body).find();
    }

    static boolean isInferenceUrl(String url) {
        String u = url.toLowerCase(Locale.ROOT);
        return u.endsWith("/chat/completions") || u.endsWith("/responses") || u.endsWith("/v1/messages")
                || u.endsWith("/messages");
    }

    static String sse(String eventType, Object data) {
        return "event: " + eventType + "\ndata: " + json(data) + "\n\n";
    }

    static String sseBody(String text, String respId) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> event : responsesEvents(text, respId)) {
            sb.append(sse((String) event.get("type"), event));
        }
        return sb.toString();
    }

    static String modelCatalog(List<String> supportedEndpoints) {
        Map<String, Object> limits = new LinkedHashMap<>();
        limits.put("max_context_window_tokens", 200000);
        limits.put("max_output_tokens", 8192);

        Map<String, Object> supports = new LinkedHashMap<>();
        supports.put("streaming", true);
        supports.put("tool_calls", true);
        supports.put("parallel_tool_calls", true);
        supports.put("vision", true);

        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("type", "chat");
        capabilities.put("family", "claude-sonnet-4.5");
        capabilities.put("tokenizer", "o200k_base");
        capabilities.put("limits", limits);
        capabilities.put("supports", supports);

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("id", "claude-sonnet-4.5");
        model.put("name", "Claude Sonnet 4.5");
        model.put("object", "model");
        model.put("vendor", "Anthropic");
        model.put("version", "1");
        model.put("preview", false);
        model.put("model_picker_enabled", true);
        model.put("capabilities", capabilities);
        if (supportedEndpoints != null) {
            model.put("supported_endpoints", supportedEndpoints);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("data", List.of(model));
        return json(root);
    }

    /**
     * Returns the ordered {@code /responses} event objects the runtime's reducer
     * expects. Used raw (one object == one WebSocket message) for the WS path and
     * SSE-framed for the HTTP path.
     */
    static List<Map<String, Object>> responsesEvents(String text, String respId) {
        Map<String, Object> created = new LinkedHashMap<>();
        created.put("type", "response.created");
        created.put("response", responseShell(respId, "in_progress", List.of()));

        Map<String, Object> itemAdded = new LinkedHashMap<>();
        itemAdded.put("type", "response.output_item.added");
        itemAdded.put("output_index", 0);
        itemAdded.put("item", message("msg_1", List.of()));

        Map<String, Object> partAdded = new LinkedHashMap<>();
        partAdded.put("type", "response.content_part.added");
        partAdded.put("output_index", 0);
        partAdded.put("content_index", 0);
        partAdded.put("part", outputText(""));

        Map<String, Object> delta = new LinkedHashMap<>();
        delta.put("type", "response.output_text.delta");
        delta.put("output_index", 0);
        delta.put("content_index", 0);
        delta.put("delta", text);

        Map<String, Object> done = new LinkedHashMap<>();
        done.put("type", "response.output_text.done");
        done.put("output_index", 0);
        done.put("content_index", 0);
        done.put("text", text);

        Map<String, Object> completedResponse = responseShell(respId, "completed",
                List.of(message("msg_1", List.of(outputText(text)))));
        completedResponse.put("usage", usage());
        Map<String, Object> completed = new LinkedHashMap<>();
        completed.put("type", "response.completed");
        completed.put("response", completedResponse);

        return List.of(created, itemAdded, partAdded, delta, done, completed);
    }

    private static Map<String, Object> responseShell(String respId, String status, List<Object> output) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", respId);
        response.put("object", "response");
        response.put("status", status);
        response.put("output", output);
        return response;
    }

    private static Map<String, Object> message(String id, List<Object> content) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("type", "message");
        item.put("role", "assistant");
        item.put("content", content);
        return item;
    }

    private static Map<String, Object> outputText(String text) {
        Map<String, Object> part = new LinkedHashMap<>();
        part.put("type", "output_text");
        part.put("text", text);
        return part;
    }

    private static Map<String, Object> usage() {
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("input_tokens", 5);
        usage.put("output_tokens", 7);
        usage.put("total_tokens", 12);
        return usage;
    }

    static String drainRequest(LlmInferenceRequest req) throws InterruptedException {
        return new String(req.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    static void respondBuffered(LlmInferenceRequest req, int status, Map<String, List<String>> headers, String body)
            throws IOException, InterruptedException {
        drainRequest(req);
        req.getResponseBody().start(new LlmInferenceResponseInit(status).setHeaders(headers));
        if (body != null && !body.isEmpty()) {
            req.getResponseBody().write(body.getBytes(StandardCharsets.UTF_8));
        }
        req.getResponseBody().end();
    }

    /**
     * Serves the model catalog, model session and policy endpoints. Returns
     * {@code true} when the request was one of those (and answered).
     */
    static boolean serviceNonInference(LlmInferenceRequest req) throws IOException, InterruptedException {
        String url = req.getUrl().toLowerCase(Locale.ROOT);
        if (url.endsWith("/models")) {
            respondBuffered(req, 200, headers("content-type", "application/json"), modelCatalog(null));
            return true;
        }
        if (url.contains("/models/session")) {
            respondBuffered(req, 200, emptyHeaders(), "{}");
            return true;
        }
        if (url.contains("/policy")) {
            respondBuffered(req, 200, emptyHeaders(), "{\"state\":\"enabled\"}");
            return true;
        }
        return false;
    }

    /**
     * Serves every non-inference model-layer request, including an empty-JSON
     * fallback for anything unrecognised.
     */
    static void handleNonInferenceModelTraffic(LlmInferenceRequest req, List<String> supportedEndpoints)
            throws IOException, InterruptedException {
        String url = req.getUrl().toLowerCase(Locale.ROOT);
        if (url.endsWith("/models")) {
            respondBuffered(req, 200, headers("content-type", "application/json"), modelCatalog(supportedEndpoints));
            return;
        }
        if (url.contains("/models/session")) {
            respondBuffered(req, 200, emptyHeaders(), "{}");
            return;
        }
        if (url.contains("/policy")) {
            respondBuffered(req, 200, emptyHeaders(), "{\"state\":\"enabled\"}");
            return;
        }
        respondBuffered(req, 200, headers("content-type", "application/json"), "{}");
    }

    /**
     * Synthesizes a well-formed inference response, dispatching by URL and the
     * request body's stream flag exactly as a real reverse proxy would.
     */
    static void handleInference(LlmInferenceRequest req, String text) throws IOException, InterruptedException {
        String body = drainRequest(req);
        boolean stream = wantsStream(body);
        String url = req.getUrl().toLowerCase(Locale.ROOT);
        LlmInferenceResponseSink sink = req.getResponseBody();

        if (url.contains("/responses")) {
            List<Map<String, Object>> events = responsesEvents(text, "resp_stub_1");
            if (!stream) {
                sink.start(new LlmInferenceResponseInit(200).setHeaders(headers("content-type", "application/json")));
                Object last = events.get(events.size() - 1).get("response");
                sink.write(json(last).getBytes(StandardCharsets.UTF_8));
                sink.end();
                return;
            }
            sink.start(new LlmInferenceResponseInit(200).setHeaders(headers("content-type", "text/event-stream")));
            for (Map<String, Object> event : events) {
                sink.write(sse((String) event.get("type"), event).getBytes(StandardCharsets.UTF_8));
            }
            sink.end();
            return;
        }

        if (url.contains("/chat/completions") && stream) {
            sink.start(new LlmInferenceResponseInit(200).setHeaders(headers("content-type", "text/event-stream")));
            for (Map<String, Object> chunk : chatCompletionChunks(text)) {
                sink.write(("data: " + json(chunk) + "\n\n").getBytes(StandardCharsets.UTF_8));
            }
            sink.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
            sink.end();
            return;
        }

        sink.start(new LlmInferenceResponseInit(200).setHeaders(headers("content-type", "application/json")));
        sink.write(json(chatCompletion(text)).getBytes(StandardCharsets.UTF_8));
        sink.end();
    }

    private static List<Map<String, Object>> chatCompletionChunks(String text) {
        Map<String, Object> c1 = chatChunkBase();
        c1.put("choices", List.of(choice(0, delta("assistant", ""), null)));
        Map<String, Object> c2 = chatChunkBase();
        c2.put("choices", List.of(choice(0, delta(null, text), null)));
        Map<String, Object> c3 = chatChunkBase();
        c3.put("choices", List.of(choice(0, new LinkedHashMap<>(), "stop")));
        c3.put("usage", chatUsage());
        return List.of(c1, c2, c3);
    }

    private static Map<String, Object> chatChunkBase() {
        Map<String, Object> base = new LinkedHashMap<>();
        base.put("id", "chatcmpl-stub-1");
        base.put("object", "chat.completion.chunk");
        base.put("created", 1);
        base.put("model", "claude-sonnet-4.5");
        return base;
    }

    private static Map<String, Object> delta(String role, String content) {
        Map<String, Object> delta = new LinkedHashMap<>();
        if (role != null) {
            delta.put("role", role);
        }
        delta.put("content", content);
        return delta;
    }

    private static Map<String, Object> choice(int index, Map<String, Object> delta, String finishReason) {
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", index);
        choice.put("delta", delta);
        choice.put("finish_reason", finishReason);
        return choice;
    }

    private static Map<String, Object> chatUsage() {
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("prompt_tokens", 5);
        usage.put("completion_tokens", 7);
        usage.put("total_tokens", 12);
        return usage;
    }

    private static Map<String, Object> chatCompletion(String text) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");
        message.put("content", text);

        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("message", message);
        choice.put("finish_reason", "stop");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", "chatcmpl-stub-1");
        root.put("object", "chat.completion");
        root.put("created", 1);
        root.put("model", "claude-sonnet-4.5");
        root.put("choices", List.of(choice));
        root.put("usage", chatUsage());
        return root;
    }

    static String assistantText(AssistantMessageEvent event) {
        if (event == null || event.getData() == null) {
            return "";
        }
        String content = event.getData().content();
        return content != null ? content : "";
    }
}
