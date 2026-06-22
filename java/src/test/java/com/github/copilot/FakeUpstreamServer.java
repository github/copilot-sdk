/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A minimal raw-socket HTTP/1.1 + RFC 6455 WebSocket upstream used by the
 * idiomatic-handler e2e test.
 * <p>
 * It serves the synthetic CAPI HTTP endpoints (model catalog, model session,
 * policy, {@code /responses} SSE) and, on a WebSocket upgrade, echoes the
 * ordered {@code /responses} events as one batch of text messages per inbound
 * message. It avoids any third-party server dependency so the test exercises
 * the real {@link java.net.http.WebSocket} forwarding path against a genuine
 * upstream.
 * </p>
 */
final class FakeUpstreamServer implements AutoCloseable {

    private static final String WS_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final ServerSocket serverSocket;
    private final Thread acceptThread;
    private final AtomicInteger upstreamWsRequests = new AtomicInteger();
    private final String httpText;
    private final String wsText;
    private volatile boolean running = true;

    FakeUpstreamServer(String httpText, String wsText) throws IOException {
        this.httpText = httpText;
        this.wsText = wsText;
        this.serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        this.acceptThread = new Thread(this::acceptLoop, "fake-upstream-accept");
        this.acceptThread.setDaemon(true);
        this.acceptThread.start();
    }

    int port() {
        return serverSocket.getLocalPort();
    }

    String httpUrl() {
        return "http://127.0.0.1:" + port();
    }

    String wsUrl() {
        return "ws://127.0.0.1:" + port();
    }

    int upstreamWsRequests() {
        return upstreamWsRequests.get();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                Thread t = new Thread(() -> handle(socket), "fake-upstream-conn");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                return;
            }
        }
    }

    private void handle(Socket socket) {
        try (socket) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            String requestLine = readLine(in);
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }
            String[] parts = requestLine.split(" ");
            String path = parts.length > 1 ? parts[1] : "/";

            Map<String, String> headers = new java.util.LinkedHashMap<>();
            String line;
            while ((line = readLine(in)) != null && !line.isEmpty()) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    headers.put(line.substring(0, colon).trim().toLowerCase(Locale.ROOT),
                            line.substring(colon + 1).trim());
                }
            }

            if ("websocket".equalsIgnoreCase(headers.get("upgrade"))) {
                serveWebSocket(in, out, headers);
                return;
            }
            serveHttp(in, out, path, headers);
        } catch (Exception ignored) {
            // Connection error; drop it.
        }
    }

    private void serveHttp(InputStream in, OutputStream out, String path, Map<String, String> headers)
            throws IOException {
        String contentLength = headers.get("content-length");
        if (contentLength != null) {
            int len = Integer.parseInt(contentLength.trim());
            byte[] body = new byte[len];
            int read = 0;
            while (read < len) {
                int n = in.read(body, read, len - read);
                if (n < 0) {
                    break;
                }
                read += n;
            }
        }

        String lower = path.toLowerCase(Locale.ROOT);
        String contentType = "application/json";
        String body;
        int status = 200;
        if (lower.endsWith("/models")) {
            body = CopilotRequestTestSupport.modelCatalog(List.of("/responses", "ws:/responses"));
        } else if (lower.contains("/models/session")) {
            body = "{}";
        } else if (lower.contains("/policy")) {
            body = "{\"state\":\"enabled\"}";
        } else if (lower.endsWith("/responses")) {
            contentType = "text/event-stream";
            body = CopilotRequestTestSupport.sseBody(httpText, "resp_stub_http");
        } else {
            status = 404;
            body = "{\"error\":\"not_found\"}";
        }

        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String header = "HTTP/1.1 " + status + " " + (status == 200 ? "OK" : "Not Found") + "\r\n" + "content-type: "
                + contentType + "\r\n" + "content-length: " + bodyBytes.length + "\r\n" + "connection: close\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.US_ASCII));
        out.write(bodyBytes);
        out.flush();
    }

    private void serveWebSocket(InputStream in, OutputStream out, Map<String, String> headers) throws Exception {
        String key = headers.get("sec-websocket-key");
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] digest = sha1.digest((key + WS_MAGIC).getBytes(StandardCharsets.US_ASCII));
        String accept = Base64.getEncoder().encodeToString(digest);
        String response = "HTTP/1.1 101 Switching Protocols\r\n" + "Upgrade: websocket\r\n" + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";
        out.write(response.getBytes(StandardCharsets.US_ASCII));
        out.flush();

        ByteArrayOutputStream message = new ByteArrayOutputStream();
        while (true) {
            int b1 = in.read();
            if (b1 < 0) {
                return;
            }
            boolean fin = (b1 & 0x80) != 0;
            int opcode = b1 & 0x0F;

            int b2 = in.read();
            if (b2 < 0) {
                return;
            }
            boolean masked = (b2 & 0x80) != 0;
            long len = b2 & 0x7F;
            if (len == 126) {
                len = ((long) in.read() << 8) | in.read();
            } else if (len == 127) {
                len = 0;
                for (int i = 0; i < 8; i++) {
                    len = (len << 8) | in.read();
                }
            }

            byte[] mask = new byte[4];
            if (masked) {
                readFully(in, mask, 4);
            }
            byte[] payload = new byte[(int) len];
            readFully(in, payload, (int) len);
            if (masked) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] ^= mask[i % 4];
                }
            }

            if (opcode == 0x8) {
                writeFrame(out, 0x8, new byte[0]);
                out.flush();
                return;
            }
            if (opcode == 0x9) {
                writeFrame(out, 0xA, payload);
                out.flush();
                continue;
            }
            if (opcode == 0x0 || opcode == 0x1 || opcode == 0x2) {
                message.writeBytes(payload);
                if (!fin) {
                    continue;
                }
                message.reset();
                upstreamWsRequests.incrementAndGet();
                for (Map<String, Object> event : CopilotRequestTestSupport.responsesEvents(wsText, "resp_stub_ws")) {
                    byte[] raw = CopilotRequestTestSupport.json(event).getBytes(StandardCharsets.UTF_8);
                    writeFrame(out, 0x1, raw);
                }
                out.flush();
            }
        }
    }

    private static void writeFrame(OutputStream out, int opcode, byte[] payload) throws IOException {
        List<Integer> bytes = new ArrayList<>();
        bytes.add(0x80 | opcode);
        int len = payload.length;
        if (len < 126) {
            bytes.add(len);
        } else if (len < 65536) {
            bytes.add(126);
            bytes.add((len >> 8) & 0xFF);
            bytes.add(len & 0xFF);
        } else {
            bytes.add(127);
            for (int i = 7; i >= 0; i--) {
                bytes.add((int) ((((long) len) >> (8 * i)) & 0xFF));
            }
        }
        byte[] header = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            header[i] = (byte) (int) bytes.get(i);
        }
        out.write(header);
        out.write(payload);
    }

    private static void readFully(InputStream in, byte[] buffer, int len) throws IOException {
        int read = 0;
        while (read < len) {
            int n = in.read(buffer, read, len - read);
            if (n < 0) {
                throw new IOException("Unexpected end of stream");
            }
            read += n;
        }
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\r') {
                int next = in.read();
                if (next == '\n' || next == -1) {
                    break;
                }
                buffer.write('\r');
                buffer.write(next);
                continue;
            }
            if (c == '\n') {
                break;
            }
            buffer.write(c);
        }
        if (c == -1 && buffer.size() == 0) {
            return null;
        }
        return buffer.toString(StandardCharsets.US_ASCII);
    }

    @Override
    public void close() throws IOException {
        running = false;
        serverSocket.close();
    }
}
