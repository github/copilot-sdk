/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.copilot.sdk.json.CopilotClientOptions;

/**
 * Manages the lifecycle of the Copilot CLI server process.
 * <p>
 * This class handles spawning the CLI server process, building command lines,
 * detecting the listening port, and establishing connections.
 */
final class CliServerManager {

    private static final Logger LOG = Logger.getLogger(CliServerManager.class.getName());

    private final CopilotClientOptions options;

    CliServerManager(CopilotClientOptions options) {
        this.options = options;
    }

    /**
     * Starts the CLI server process.
     *
     * @return information about the started process including detected port
     * @throws IOException
     *             if the process cannot be started
     * @throws InterruptedException
     *             if interrupted while waiting for port detection
     */
    ProcessInfo startCliServer() throws IOException, InterruptedException {
        String cliPath = options.getCliPath() != null ? options.getCliPath() : "copilot";
        var args = new ArrayList<String>();

        if (options.getCliArgs() != null) {
            args.addAll(Arrays.asList(options.getCliArgs()));
        }

        args.add("--server");
        args.add("--log-level");
        args.add(options.getLogLevel());

        if (options.isUseStdio()) {
            args.add("--stdio");
        } else if (options.getPort() > 0) {
            args.add("--port");
            args.add(String.valueOf(options.getPort()));
        }

        // Add auth-related flags
        if (options.getGithubToken() != null && !options.getGithubToken().isEmpty()) {
            args.add("--auth-token-env");
            args.add("COPILOT_SDK_AUTH_TOKEN");
        }

        // Default UseLoggedInUser to false when GithubToken is provided
        boolean useLoggedInUser = options.getUseLoggedInUser() != null
                ? options.getUseLoggedInUser()
                : (options.getGithubToken() == null || options.getGithubToken().isEmpty());
        if (!useLoggedInUser) {
            args.add("--no-auto-login");
        }

        List<String> command = resolveCliCommand(cliPath, args);

        var pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        if (options.getCwd() != null) {
            pb.directory(new File(options.getCwd()));
        }

        if (options.getEnvironment() != null) {
            pb.environment().clear();
            pb.environment().putAll(options.getEnvironment());
        }
        pb.environment().remove("NODE_DEBUG");

        // Set auth token in environment if provided
        if (options.getGithubToken() != null && !options.getGithubToken().isEmpty()) {
            pb.environment().put("COPILOT_SDK_AUTH_TOKEN", options.getGithubToken());
        }

        Process process = pb.start();

        // Forward stderr to logger in background
        startStderrReader(process);

        Integer detectedPort = null;
        if (!options.isUseStdio()) {
            detectedPort = waitForPortAnnouncement(process);
        }

        return new ProcessInfo(process, detectedPort);
    }

    /**
     * Connects to a running Copilot server.
     *
     * @param process
     *            the CLI process (null if connecting to external server)
     * @param tcpHost
     *            the host to connect to (null for stdio mode)
     * @param tcpPort
     *            the port to connect to (null for stdio mode)
     * @return the JSON-RPC client connected to the server
     * @throws IOException
     *             if connection fails
     */
    JsonRpcClient connectToServer(Process process, String tcpHost, Integer tcpPort) throws IOException {
        if (options.isUseStdio()) {
            if (process == null) {
                throw new IllegalStateException("CLI process not started");
            }
            return JsonRpcClient.fromProcess(process);
        } else {
            if (tcpHost == null || tcpPort == null) {
                throw new IllegalStateException("Cannot connect because TCP host or port are not available");
            }
            Socket socket = new Socket(tcpHost, tcpPort);
            return JsonRpcClient.fromSocket(socket);
        }
    }

    private void startStderrReader(Process process) {
        var stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.fine("[CLI] " + line);
                }
            } catch (IOException e) {
                LOG.log(Level.FINE, "Error reading stderr", e);
            }
        }, "cli-stderr-reader");
        stderrThread.setDaemon(true);
        stderrThread.start();
    }

    private Integer waitForPortAnnouncement(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            Pattern portPattern = Pattern.compile("listening on port (\\d+)", Pattern.CASE_INSENSITIVE);
            long deadline = System.currentTimeMillis() + 30000;

            while (System.currentTimeMillis() < deadline) {
                String line = reader.readLine();
                if (line == null) {
                    throw new IOException("CLI process exited unexpectedly");
                }

                Matcher matcher = portPattern.matcher(line);
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                }
            }

            process.destroyForcibly();
            throw new IOException("Timeout waiting for CLI to announce port");
        }
    }

    private List<String> resolveCliCommand(String cliPath, List<String> args) {
        boolean isJsFile = cliPath.toLowerCase().endsWith(".js");

        if (isJsFile) {
            var result = new ArrayList<String>();
            result.add("node");
            result.add(cliPath);
            result.addAll(args);
            return result;
        }

        // On Windows, use cmd /c to resolve the executable
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win") && !new File(cliPath).isAbsolute()) {
            var result = new ArrayList<String>();
            result.add("cmd");
            result.add("/c");
            result.add(cliPath);
            result.addAll(args);
            return result;
        }

        var result = new ArrayList<String>();
        result.add(cliPath);
        result.addAll(args);
        return result;
    }

    static URI parseCliUrl(String url) {
        // If it's just a port number, treat as localhost
        try {
            int port = Integer.parseInt(url);
            return URI.create("http://localhost:" + port);
        } catch (NumberFormatException e) {
            // Not a port number, continue
        }

        // Add scheme if missing
        if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
            url = "https://" + url;
        }

        return URI.create(url);
    }

    /**
     * Information about a started CLI server process.
     *
     * @param process
     *            the CLI process
     * @param port
     *            the detected TCP port (null for stdio mode)
     */
    record ProcessInfo(Process process, Integer port) {
    }
}
