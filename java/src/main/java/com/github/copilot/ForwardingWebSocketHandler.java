/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * The default {@link CopilotWebSocketHandler}: it dials the real upstream using
 * {@link java.net.http.WebSocket} and forwards upstream-to-runtime messages
 * into the response writer.
 * <p>
 * Subclass and override {@link #onSendRequestMessage} or
 * {@link #onSendResponseMessage} to observe, transform, or drop messages in
 * either direction.
 *
 * @since 1.0.0
 */
public class ForwardingWebSocketHandler implements CopilotWebSocketHandler {

    private final String url;
    private final Map<String, List<String>> headers;
    private final CompletableFuture<Void> completion = new CompletableFuture<>();

    private volatile WebSocket webSocket;
    private volatile WebSocketResponseWriter responseWriter;

    /**
     * Creates a forwarding handler targeting {@code url} with the given handshake
     * headers.
     *
     * @param url
     *            the upstream WebSocket URL
     * @param headers
     *            the handshake headers, multi-valued
     */
    public ForwardingWebSocketHandler(String url, Map<String, List<String>> headers) {
        this.url = url;
        this.headers = headers;
    }

    /**
     * Observes or transforms each runtime-to-upstream message. The default returns
     * the data unchanged. Return {@code null} to drop the message.
     *
     * @param data
     *            the message bytes
     * @param binary
     *            whether the message was delivered as binary
     * @return the bytes to forward upstream, or {@code null} to drop
     */
    protected byte[] onSendRequestMessage(byte[] data, boolean binary) {
        return data;
    }

    /**
     * Observes or transforms each upstream-to-runtime message. The default returns
     * the data unchanged. Return {@code null} to drop the message.
     *
     * @param data
     *            the message bytes
     * @param binary
     *            whether the message was received as binary
     * @return the bytes to forward to the runtime, or {@code null} to drop
     */
    protected byte[] onSendResponseMessage(byte[] data, boolean binary) {
        return data;
    }

    @Override
    public void open(WebSocketResponseWriter responseWriter) throws Exception {
        this.responseWriter = responseWriter;
        WebSocket.Builder builder = HttpClient.newHttpClient().newWebSocketBuilder();
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (LlmRequestHandler.isForbiddenRequestHeader(entry.getKey()) || entry.getValue() == null) {
                    continue;
                }
                for (String value : entry.getValue()) {
                    builder.header(entry.getKey(), value);
                }
            }
        }
        try {
            this.webSocket = builder.buildAsync(URI.create(normalizeWebSocketScheme(url)), new ForwardingListener())
                    .join();
        } catch (Exception e) {
            throw unwrap(e);
        }
    }

    @Override
    public void sendRequestMessage(byte[] data, boolean binary) throws Exception {
        byte[] out = onSendRequestMessage(data, binary);
        if (out == null) {
            return;
        }
        WebSocket ws = this.webSocket;
        if (ws == null) {
            return;
        }
        if (binary) {
            ws.sendBinary(ByteBuffer.wrap(out), true).join();
        } else {
            ws.sendText(new String(out, StandardCharsets.UTF_8), true).join();
        }
    }

    @Override
    public CompletableFuture<Void> completion() {
        return completion;
    }

    @Override
    public void close() {
        WebSocket ws = this.webSocket;
        if (ws != null && !ws.isOutputClosed()) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "").exceptionally(ex -> null);
        }
    }

    private static String normalizeWebSocketScheme(String url) {
        if (url.startsWith("http://")) {
            return "ws://" + url.substring("http://".length());
        }
        if (url.startsWith("https://")) {
            return "wss://" + url.substring("https://".length());
        }
        return url;
    }

    private static Exception unwrap(Exception e) {
        Throwable cause = e.getCause();
        if (cause instanceof Exception ex) {
            return ex;
        }
        return e;
    }

    private void forward(byte[] data, boolean binary) {
        byte[] out = onSendResponseMessage(data, binary);
        if (out == null) {
            return;
        }
        WebSocketResponseWriter writer = this.responseWriter;
        if (writer == null) {
            return;
        }
        try {
            if (binary) {
                writer.sendBinary(out);
            } else {
                writer.sendText(out);
            }
        } catch (Exception e) {
            completion.completeExceptionally(e);
        }
    }

    private final class ForwardingListener implements WebSocket.Listener {

        private final StringBuilder textBuffer = new StringBuilder();
        private final ByteArrayOutputStream binaryBuffer = new ByteArrayOutputStream();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(Long.MAX_VALUE);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                byte[] message = textBuffer.toString().getBytes(StandardCharsets.UTF_8);
                textBuffer.setLength(0);
                forward(message, false);
            }
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            byte[] chunk = new byte[data.remaining()];
            data.get(chunk);
            binaryBuffer.writeBytes(chunk);
            if (last) {
                byte[] message = binaryBuffer.toByteArray();
                binaryBuffer.reset();
                forward(message, true);
            }
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            completion.complete(null);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            completion.completeExceptionally(error);
        }
    }
}
