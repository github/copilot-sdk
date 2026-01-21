/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.PingResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CopilotClient.
 *
 * Note: These tests require the Copilot CLI to be installed. Set the
 * COPILOT_CLI_PATH environment variable to the path to the CLI, or run 'npm
 * install' in the nodejs directory.
 */
public class CopilotClientTest {

    private static String cliPath;

    @BeforeAll
    static void setup() {
        cliPath = getCliPath();
    }

    private static String getCliPath() {
        // First, try to find 'copilot' in PATH
        String copilotInPath = findCopilotInPath();
        if (copilotInPath != null) {
            return copilotInPath;
        }

        // Fall back to COPILOT_CLI_PATH environment variable
        String envPath = System.getenv("COPILOT_CLI_PATH");
        if (envPath != null && !envPath.isEmpty()) {
            return envPath;
        }

        // Search for the CLI in the parent directories (nodejs module)
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path cliPath = current.resolve("nodejs/node_modules/@github/copilot/index.js");
            if (cliPath.toFile().exists()) {
                return cliPath.toString();
            }
            current = current.getParent();
        }

        return null;
    }

    private static String findCopilotInPath() {
        try {
            // Use 'where' on Windows, 'which' on Unix-like systems
            String command = System.getProperty("os.name").toLowerCase().contains("win") ? "where" : "which";
            ProcessBuilder pb = new ProcessBuilder(command, "copilot");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                int exitCode = process.waitFor();
                if (exitCode == 0 && line != null && !line.isEmpty()) {
                    return line.trim();
                }
            }
        } catch (Exception e) {
            // Ignore - copilot not found in PATH
        }
        return null;
    }

    @Test
    void testClientConstruction() {
        CopilotClient client = new CopilotClient();
        assertEquals(ConnectionState.DISCONNECTED, client.getState());
        client.close();
    }

    @Test
    void testClientConstructionWithOptions() {
        CopilotClientOptions options = new CopilotClientOptions().setCliPath("/path/to/cli").setLogLevel("debug")
                .setAutoStart(false);

        CopilotClient client = new CopilotClient(options);
        assertEquals(ConnectionState.DISCONNECTED, client.getState());
        client.close();
    }

    @Test
    void testCliUrlMutualExclusion() {
        CopilotClientOptions options = new CopilotClientOptions().setCliUrl("localhost:3000").setUseStdio(true);

        assertThrows(IllegalArgumentException.class, () -> new CopilotClient(options));
    }

    @Test
    void testCliUrlMutualExclusionWithCliPath() {
        CopilotClientOptions options = new CopilotClientOptions().setCliUrl("localhost:3000").setCliPath("/path/to/cli")
                .setUseStdio(false);

        assertThrows(IllegalArgumentException.class, () -> new CopilotClient(options));
    }

    @Test
    void testStartAndConnectUsingStdio() throws Exception {
        if (cliPath == null) {
            System.out.println("Skipping test: CLI not found");
            return;
        }

        try (var client = new CopilotClient(new CopilotClientOptions().setCliPath(cliPath).setUseStdio(true))) {
            client.start().get();
            assertEquals(ConnectionState.CONNECTED, client.getState());

            PingResponse pong = client.ping("test message").get();
            assertEquals("pong: test message", pong.getMessage());
            assertTrue(pong.getTimestamp() >= 0);

            client.stop().get();
            assertEquals(ConnectionState.DISCONNECTED, client.getState());
        }
    }

    @Test
    void testStartAndConnectUsingTcp() throws Exception {
        if (cliPath == null) {
            System.out.println("Skipping test: CLI not found");
            return;
        }

        try (var client = new CopilotClient(new CopilotClientOptions().setCliPath(cliPath).setUseStdio(false))) {
            client.start().get();
            assertEquals(ConnectionState.CONNECTED, client.getState());

            PingResponse pong = client.ping("test message").get();
            assertEquals("pong: test message", pong.getMessage());

            client.stop().get();
        }
    }

    @Test
    void testForceStopWithoutCleanup() throws Exception {
        if (cliPath == null) {
            System.out.println("Skipping test: CLI not found");
            return;
        }

        try (var client = new CopilotClient(new CopilotClientOptions().setCliPath(cliPath))) {
            client.createSession().get();
            client.forceStop().get();

            assertEquals(ConnectionState.DISCONNECTED, client.getState());
        }
    }
}
