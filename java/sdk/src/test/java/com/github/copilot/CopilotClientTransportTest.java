/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.InProcessRuntimeConnection;
import com.github.copilot.rpc.RuntimeConnection;
import com.github.copilot.rpc.StdioRuntimeConnection;
import com.github.copilot.rpc.TcpRuntimeConnection;
import com.github.copilot.rpc.TelemetryConfig;
import com.github.copilot.rpc.UriRuntimeConnection;

/**
 * Unit tests for transport selection through {@link RuntimeConnection}: the
 * in-process code path, {@code COPILOT_SDK_DEFAULT_CONNECTION} resolution, the
 * backward-compatibility bridge from the individual transport options, and
 * option validation.
 */
@AllowCopilotExperimental
class CopilotClientTransportTest {

    // ===== In-process routing =====

    @Test
    void inProcessConnectionStartsThroughInProcessRuntimeHost() throws Exception {
        var options = new CopilotClientOptions().setConnection(RuntimeConnection.forInProcess());
        try (var runtime = new FakeInProcessRuntime(); var client = new CopilotClient(options)) {
            client.setInProcessTransportFactory(runtime::open);

            client.start().get(30, TimeUnit.SECONDS);

            assertTrue(runtime.opened.get(), "The in-process runtime must be used for an in-process connection");
            assertInstanceOf(InProcessRuntimeConnection.class, client.getRuntimeConnection());

            client.stop().get(30, TimeUnit.SECONDS);
            assertTrue(runtime.closed.get(), "Stopping the client must close the in-process runtime host");
        }
    }

    @Test
    void inProcessStartupFailurePropagates() {
        var options = new CopilotClientOptions().setConnection(RuntimeConnection.forInProcess());
        try (var client = new CopilotClient(options)) {
            client.setInProcessTransportFactory(opts -> {
                throw new IOException("no runtime available");
            });
            var failure = assertThrows(Exception.class, () -> client.start().get(30, TimeUnit.SECONDS));
            assertTrue(rootMessage(failure).contains("no runtime available"));
        }
    }

    @Test
    void cliTransportDoesNotUseTheInProcessRuntime() throws Exception {
        var options = new CopilotClientOptions().setCliUrl("127.0.0.1:1");
        try (var client = new CopilotClient(options)) {
            client.setInProcessTransportFactory(opts -> {
                throw new AssertionError("The in-process runtime must not be used for a CLI transport");
            });

            assertThrows(Exception.class, () -> client.start().get(30, TimeUnit.SECONDS));
            assertInstanceOf(UriRuntimeConnection.class, client.getRuntimeConnection());
        }
    }

    // ===== COPILOT_SDK_DEFAULT_CONNECTION resolution =====

    @Test
    void defaultConnectionEnvVarSelectsInProcess() {
        var connection = CopilotClient.resolveDefaultConnection(new CopilotClientOptions(), "inprocess");
        assertInstanceOf(InProcessRuntimeConnection.class, connection);
        assertInstanceOf(InProcessRuntimeConnection.class,
                CopilotClient.resolveDefaultConnection(new CopilotClientOptions(), "InProcess"));
    }

    @Test
    void defaultConnectionEnvVarStdioAndUnsetKeepTheConfiguredTransport() {
        assertInstanceOf(StdioRuntimeConnection.class,
                CopilotClient.resolveDefaultConnection(new CopilotClientOptions(), "stdio"));
        assertInstanceOf(StdioRuntimeConnection.class,
                CopilotClient.resolveDefaultConnection(new CopilotClientOptions(), null));
        assertInstanceOf(TcpRuntimeConnection.class,
                CopilotClient.resolveDefaultConnection(new CopilotClientOptions().setUseStdio(false), ""));
        assertInstanceOf(TcpRuntimeConnection.class, CopilotClient.resolveDefaultConnection(
                new CopilotClientOptions().setUseStdio(false).setTcpConnectionToken("secret"), "inprocess"));
    }

    @Test
    void defaultConnectionEnvVarRejectsUnknownValues() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> CopilotClient.resolveDefaultConnection(new CopilotClientOptions(), "websocket"));
        assertTrue(error.getMessage().contains(CopilotClient.DEFAULT_CONNECTION_ENV_VAR));
    }

    // ===== Backward-compatibility bridge =====

    @Test
    void legacyStdioOptionsInferStdioConnection() {
        try (var client = new CopilotClient(new CopilotClientOptions().setCliPath("/usr/local/bin/copilot"))) {
            var connection = assertInstanceOf(StdioRuntimeConnection.class, client.getRuntimeConnection());
            assertEquals("/usr/local/bin/copilot", connection.getPath());
        }
    }

    @Test
    void legacyTcpOptionsInferTcpConnection() {
        var options = new CopilotClientOptions().setUseStdio(false).setPort(4321).setTcpConnectionToken("secret");
        try (var client = new CopilotClient(options)) {
            var connection = assertInstanceOf(TcpRuntimeConnection.class, client.getRuntimeConnection());
            assertEquals(4321, connection.getPort());
            assertEquals("secret", connection.getConnectionToken());
        }
    }

    @Test
    void legacyCliUrlInfersUriConnection() {
        try (var client = new CopilotClient(new CopilotClientOptions().setCliUrl("localhost:3000"))) {
            var connection = assertInstanceOf(UriRuntimeConnection.class, client.getRuntimeConnection());
            assertEquals("localhost:3000", connection.getUrl());
        }
    }

    // ===== Connection applied to the transport options =====

    @Test
    void connectionIsProjectedOntoTransportOptions() {
        var stdio = new CopilotClientOptions().setConnection(RuntimeConnection.forStdio("/opt/copilot"));
        try (var client = new CopilotClient(stdio)) {
            assertTrue(stdio.isUseStdio());
            assertEquals("/opt/copilot", stdio.getCliPath());
        }

        var tcp = new CopilotClientOptions().setConnection(
                RuntimeConnection.forTcp().setPort(4321).setConnectionToken("secret").setArgs(List.of("--extra")));
        try (var client = new CopilotClient(tcp)) {
            assertFalse(tcp.isUseStdio());
            assertEquals(4321, tcp.getPort());
            assertEquals("secret", tcp.getTcpConnectionToken());
            assertEquals(List.of("--extra"), List.of(tcp.getCliArgs()));
        }

        var uri = new CopilotClientOptions().setConnection(RuntimeConnection.forUri("localhost:3000"));
        try (var client = new CopilotClient(uri)) {
            assertFalse(uri.isUseStdio());
            assertEquals("localhost:3000", uri.getCliUrl());
        }
    }

    // ===== Conflicting configuration =====

    @Test
    void connectionCannotBeCombinedWithTransportOptions() {
        assertConflict(new CopilotClientOptions().setConnection(RuntimeConnection.forStdio())
                .setCliPath("/usr/local/bin/copilot"), "CliPath");
        assertConflict(
                new CopilotClientOptions().setConnection(RuntimeConnection.forInProcess()).setCliUrl("localhost:3000"),
                "CliUrl");
        assertConflict(new CopilotClientOptions().setConnection(RuntimeConnection.forStdio()).setUseStdio(false),
                "UseStdio");
        assertConflict(new CopilotClientOptions().setConnection(RuntimeConnection.forTcp()).setPort(4321), "Port");
        assertConflict(
                new CopilotClientOptions().setConnection(RuntimeConnection.forTcp()).setTcpConnectionToken("secret"),
                "TcpConnectionToken");
        assertConflict(new CopilotClientOptions().setConnection(RuntimeConnection.forStdio())
                .setCliArgs(new String[]{"--extra"}), "CliArgs");
    }

    @Test
    void connectionCanBeReusedForSeveralClients() {
        var options = new CopilotClientOptions().setConnection(RuntimeConnection.forStdio("/opt/copilot"));
        try (var first = new CopilotClient(options); var second = new CopilotClient(options)) {
            assertInstanceOf(StdioRuntimeConnection.class, first.getRuntimeConnection());
            assertInstanceOf(StdioRuntimeConnection.class, second.getRuntimeConnection());
        }
    }

    private static void assertConflict(CopilotClientOptions options, String optionName) {
        var error = assertThrows(IllegalArgumentException.class, () -> new CopilotClient(options));
        assertTrue(error.getMessage().contains(optionName), "Expected '" + optionName + "' in: " + error.getMessage());
    }

    // ===== Options rejected for the in-process transport =====

    @Test
    void inProcessRejectsPerProcessOptions() {
        assertInProcessRejected(new CopilotClientOptions().setEnvironment(Map.of("FOO", "bar")), "Environment");
        assertInProcessRejected(new CopilotClientOptions().setTelemetry(new TelemetryConfig()), "Telemetry");
        assertInProcessRejected(new CopilotClientOptions().setCwd("/tmp"), "Cwd");
        assertInProcessRejected(new CopilotClientOptions().setCliArgs(new String[]{"--extra"}), "CliArgs");
    }

    @Test
    void e2eContextClearsInProcessIncompatibleOptions() throws Exception {
        try (var context = E2ETestContext.create()) {
            var options = new CopilotClientOptions().setConnection(RuntimeConnection.forInProcess())
                    .setEnvironment(Map.of("TEST_KEY", "test-value")).setCwd(context.getWorkDir().toString())
                    .setCliArgs(new String[]{"--subprocess-only"});

            try (var client = context.createClient(options)) {
                assertInstanceOf(InProcessRuntimeConnection.class, client.getRuntimeConnection());
                assertTrue(options.getEnvironment() == null || options.getEnvironment().isEmpty());
                assertEquals(null, options.getCwd());
                assertTrue(options.getCliArgs() == null || options.getCliArgs().length == 0);
            }
        }
    }

    private static void assertInProcessRejected(CopilotClientOptions options, String optionName) {
        options.setConnection(RuntimeConnection.forInProcess());
        var error = assertThrows(IllegalArgumentException.class, () -> new CopilotClient(options));
        assertTrue(error.getMessage().contains(optionName), "Expected '" + optionName + "' in: " + error.getMessage());
        assertTrue(error.getMessage().contains("forInProcess"),
                "Expected the in-process transport to be named in: " + error.getMessage());
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return String.valueOf(cause.getMessage());
    }

    /**
     * Minimal loopback stand-in for the in-process runtime: it speaks just enough
     * JSON-RPC for {@link CopilotClient#start()} to complete, so the test can
     * assert that the client wires its transport to the in-process host rather than
     * to a child process.
     */
    private static final class FakeInProcessRuntime implements AutoCloseable {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        private final AtomicBoolean opened = new AtomicBoolean();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final BytePipe toClient;
        private final BytePipe toRuntime;
        private final InputStream runtimeInput;
        private final OutputStream runtimeOutput;
        private final Thread responder;

        FakeInProcessRuntime() throws IOException {
            this.toClient = new BytePipe();
            this.toRuntime = new BytePipe();
            this.runtimeInput = toRuntime.inputStream();
            this.runtimeOutput = toClient.outputStream();
            this.responder = new Thread(this::respondToRequests, "fake-inprocess-runtime");
            this.responder.setDaemon(true);
            this.responder.start();
        }

        CopilotClient.InProcessTransport open(CopilotClientOptions options) {
            opened.set(true);
            return new CopilotClient.InProcessTransport(toClient.inputStream(), toRuntime.outputStream(), this::close);
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            toRuntime.close();
            toClient.close();
        }

        private void respondToRequests() {
            try {
                while (!closed.get()) {
                    JsonNode request = readMessage(runtimeInput);
                    if (request == null) {
                        return;
                    }
                    if (!request.hasNonNull("id")) {
                        continue;
                    }
                    var response = MAPPER.createObjectNode();
                    response.put("jsonrpc", "2.0");
                    response.set("id", request.get("id"));
                    var result = response.putObject("result");
                    if ("connect".equals(request.path("method").asText())) {
                        result.put("protocolVersion", SdkProtocolVersion.get());
                    }
                    writeMessage(runtimeOutput, response);
                }
            } catch (IOException e) {
                // The streams are closed when the client shuts down.
            }
        }

        private static JsonNode readMessage(InputStream in) throws IOException {
            int contentLength = -1;
            var line = new ByteArrayOutputStream();
            while (true) {
                int b = in.read();
                if (b == -1) {
                    return null;
                }
                if (b == '\n') {
                    String header = line.toString(StandardCharsets.UTF_8).trim();
                    line.reset();
                    if (header.isEmpty()) {
                        break;
                    }
                    if (header.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
                        contentLength = Integer.parseInt(header.substring(header.indexOf(':') + 1).trim());
                    }
                } else if (b != '\r') {
                    line.write(b);
                }
            }
            if (contentLength < 0) {
                throw new IOException("Missing Content-Length header");
            }
            byte[] body = in.readNBytes(contentLength);
            if (body.length != contentLength) {
                return null;
            }
            return MAPPER.readTree(body);
        }

        private static void writeMessage(OutputStream out, JsonNode message) throws IOException {
            byte[] body = MAPPER.writeValueAsBytes(message);
            out.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(body);
            out.flush();
        }
    }

    /**
     * Duplex byte channel used by {@link FakeInProcessRuntime} to emulate the
     * streams of an in-process runtime.
     */
    private static final class BytePipe {

        private final Pipe pipe;

        BytePipe() throws IOException {
            this.pipe = Pipe.open();
        }

        InputStream inputStream() {
            return Channels.newInputStream(pipe.source());
        }

        OutputStream outputStream() {
            return Channels.newOutputStream(pipe.sink());
        }

        void close() {
            closeQuietly(pipe.sink());
            closeQuietly(pipe.source());
        }

        private static void closeQuietly(Closeable closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Nothing useful to do while tearing down a test pipe.
            }
        }
    }
}
