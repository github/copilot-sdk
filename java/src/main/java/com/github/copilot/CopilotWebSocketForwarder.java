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
import java.util.concurrent.CompletionStage;

/**
 * The default pass-through {@link CopilotWebSocketHandler}: it dials the real
 * upstream using {@link java.net.http.WebSocket} and relays upstream-to-runtime
 * messages into the runtime response unchanged.
 * <p>
 * Subclass and override {@link #sendRequestMessage} or
 * {@link #sendResponseMessage} (calling {@code super}) to observe, transform,
 * or drop messages in either direction.
 *
 * @since 1.0.0
 */
public class CopilotWebSocketForwarder extends CopilotWebSocketHandler {

    private volatile WebSocket webSocket;

    /**
     * Creates a forwarding handler targeting the request URL and headers from
     * {@code context}.
     *
     * @param context
     *            the per-request context
     */
    public CopilotWebSocketForwarder(CopilotRequestContext context) {
        super(context);
    }

    @Override
    void open() throws Exception {
        if (webSocket != null) {
            return;
        }
        WebSocket.Builder builder = HttpClient.newHttpClient().newWebSocketBuilder();
        Map<String, List<String>> headers = context.headers();
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (CopilotRequestHandler.isForbiddenRequestHeader(entry.getKey()) || entry.getValue() == null) {
                    continue;
                }
                for (String value : entry.getValue()) {
                    builder.header(entry.getKey(), value);
                }
            }
        }
        try {
            this.webSocket = builder
                    .buildAsync(URI.create(normalizeWebSocketScheme(context.url())), new ForwardingListener()).join();
        } catch (Exception e) {
            throw unwrap(e);
        }
    }

    @Override
    public void sendRequestMessage(CopilotWebSocketMessage message) throws Exception {
        WebSocket ws = this.webSocket;
        if (ws == null) {
            return;
        }
        if (message.binary()) {
            ws.sendBinary(ByteBuffer.wrap(message.data()), true).join();
        } else {
            ws.sendText(message.text(), true).join();
        }
    }

    @Override
    public void close(CopilotWebSocketCloseStatus status) throws Exception {
        WebSocket ws = this.webSocket;
        if (ws != null && !ws.isOutputClosed()) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "").exceptionally(ex -> null);
        }
        super.close(status);
    }

    private void forward(byte[] data, boolean binary) {
        try {
            sendResponseMessage(new CopilotWebSocketMessage(data, binary));
        } catch (Exception e) {
            completion().completeExceptionally(e);
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
            close();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            try {
                close(new CopilotWebSocketCloseStatus(error.getMessage(), null, error));
            } catch (Exception e) {
                completion().completeExceptionally(e);
            }
        }
    }
}
