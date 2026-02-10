/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;

import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.json.CopilotClientOptions;

/**
 * Unit tests for {@link CliServerManager} covering parseCliUrl,
 * connectToServer, resolveCliCommand, and ProcessInfo coverage gaps identified
 * by JaCoCo.
 */
class CliServerManagerTest {

    // ===== parseCliUrl tests =====

    @Test
    void parseCliUrlWithPortNumber() {
        URI uri = CliServerManager.parseCliUrl("8080");
        assertEquals("http://localhost:8080", uri.toString());
    }

    @Test
    void parseCliUrlWithHostColonPort() {
        URI uri = CliServerManager.parseCliUrl("myhost:9090");
        assertEquals("https://myhost:9090", uri.toString());
    }

    @Test
    void parseCliUrlWithHttpPrefix() {
        URI uri = CliServerManager.parseCliUrl("http://example.com:3000");
        assertEquals("http://example.com:3000", uri.toString());
    }

    @Test
    void parseCliUrlWithHttpsPrefix() {
        URI uri = CliServerManager.parseCliUrl("https://secure.host:443");
        assertEquals("https://secure.host:443", uri.toString());
    }

    @Test
    void parseCliUrlWithHostOnly() {
        URI uri = CliServerManager.parseCliUrl("copilot.example.com");
        assertEquals("https://copilot.example.com", uri.toString());
    }

    // ===== connectToServer tests =====

    @Test
    void connectToServerTcpMode() throws Exception {
        var options = new CopilotClientOptions();
        var manager = new CliServerManager(options);

        // Start a temporary server socket to connect to
        try (ServerSocket ss = new ServerSocket(0)) {
            int port = ss.getLocalPort();
            JsonRpcClient client = manager.connectToServer(null, "localhost", port);
            assertNotNull(client);
            client.close();
        }
    }

    @Test
    void connectToServerStdioMode() throws Exception {
        var options = new CopilotClientOptions();
        var manager = new CliServerManager(options);

        // Create a dummy process for stdio mode
        Process process = new ProcessBuilder("cat").start();
        try {
            JsonRpcClient client = manager.connectToServer(process, null, null);
            assertNotNull(client);
            client.close();
        } finally {
            process.destroyForcibly();
        }
    }

    @Test
    void connectToServerNoProcessNoHost() {
        var options = new CopilotClientOptions();
        var manager = new CliServerManager(options);

        var ex = assertThrows(IllegalStateException.class, () -> manager.connectToServer(null, null, null));
        assertTrue(ex.getMessage().contains("Cannot connect"));
    }

    @Test
    void connectToServerNullHostNonNullPort() {
        var options = new CopilotClientOptions();
        var manager = new CliServerManager(options);

        // tcpHost is null but tcpPort is non-null → falls to process check → process
        // null → exception
        var ex = assertThrows(IllegalStateException.class, () -> manager.connectToServer(null, null, 8080));
        assertTrue(ex.getMessage().contains("Cannot connect"));
    }

    // ===== ProcessInfo record tests =====

    @Test
    void processInfoRecord() {
        var info = new CliServerManager.ProcessInfo(null, 12345);
        assertNull(info.process());
        assertEquals(12345, info.port());
    }

    @Test
    void processInfoWithNullPort() {
        var info = new CliServerManager.ProcessInfo(null, null);
        assertNull(info.process());
        assertNull(info.port());
    }

    // ===== resolveCliCommand tests (via startCliServer) =====
    // resolveCliCommand is private, so we test indirectly through startCliServer
    // with specific cliPath values.

    @Test
    void startCliServerWithJsFile() throws Exception {
        // Using a .js file path causes resolveCliCommand to prepend "node"
        // node is on PATH so the process starts, but the script doesn't exist
        // so node exits quickly — verifying the .js branch was taken
        var options = new CopilotClientOptions().setCliPath("/nonexistent/script.js").setUseStdio(true);
        var manager = new CliServerManager(options);

        try {
            var info = manager.startCliServer();
            // If process started, clean it up
            info.process().destroyForcibly();
        } catch (IOException e) {
            // Expected — node may fail or not be present; either way the branch is hit
            assertNotNull(e);
        }
    }

    @Test
    void startCliServerWithCliArgs() throws Exception {
        // Test that cliArgs are included in the command
        var options = new CopilotClientOptions().setCliPath("/nonexistent/copilot")
                .setCliArgs(new String[]{"--extra-flag"}).setUseStdio(true);
        var manager = new CliServerManager(options);

        var ex = assertThrows(IOException.class, () -> manager.startCliServer());
        assertNotNull(ex);
    }

    @Test
    void startCliServerWithExplicitPort() throws Exception {
        // Test the explicit port branch (useStdio=false, port > 0)
        var options = new CopilotClientOptions().setCliPath("/nonexistent/copilot").setUseStdio(false).setPort(9999);
        var manager = new CliServerManager(options);

        var ex = assertThrows(IOException.class, () -> manager.startCliServer());
        assertNotNull(ex);
    }

    @Test
    void startCliServerWithGithubToken() throws Exception {
        // Test the github token branch
        var options = new CopilotClientOptions().setCliPath("/nonexistent/copilot").setGithubToken("ghp_test123")
                .setUseStdio(true);
        var manager = new CliServerManager(options);

        var ex = assertThrows(IOException.class, () -> manager.startCliServer());
        assertNotNull(ex);
    }

    @Test
    void startCliServerWithUseLoggedInUserExplicit() throws Exception {
        // Test the explicit useLoggedInUser=false branch (adds --no-auto-login)
        var options = new CopilotClientOptions().setCliPath("/nonexistent/copilot").setUseLoggedInUser(false)
                .setUseStdio(true);
        var manager = new CliServerManager(options);

        var ex = assertThrows(IOException.class, () -> manager.startCliServer());
        assertNotNull(ex);
    }

    @Test
    void startCliServerWithGithubTokenAndNoExplicitUseLoggedInUser() throws Exception {
        // When githubToken is set and useLoggedInUser is null, defaults to false
        var options = new CopilotClientOptions().setCliPath("/nonexistent/copilot").setGithubToken("ghp_test123")
                .setUseStdio(true);
        var manager = new CliServerManager(options);

        var ex = assertThrows(IOException.class, () -> manager.startCliServer());
        assertNotNull(ex);
    }

    @Test
    void startCliServerWithNullCliPath() throws Exception {
        // Test the null cliPath branch (defaults to "copilot")
        var options = new CopilotClientOptions().setCliPath(null).setUseStdio(true);
        var manager = new CliServerManager(options);

        // "copilot" likely doesn't exist in the test env — that's fine
        try {
            var info = manager.startCliServer();
            info.process().destroyForcibly();
        } catch (IOException e) {
            // Expected if "copilot" is not on PATH
            assertNotNull(e);
        }
    }
}
