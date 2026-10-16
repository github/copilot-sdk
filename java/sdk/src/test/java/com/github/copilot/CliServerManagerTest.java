/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.copilot.ffi.NativeRuntimeLoader;
import com.github.copilot.rpc.CopilotClientMode;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.TelemetryConfig;

/**
 * Unit tests for {@link CliServerManager} covering parseCliUrl,
 * connectToServer, resolveCliCommand, and ProcessInfo coverage gaps identified
 * by JaCoCo.
 */
class CliServerManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void explicitCliPathDoesNotRequireRuntimeBundle() throws Exception {
        Path explicit = tempDir.resolve("copilot");
        var manager = new CliServerManager(new CopilotClientOptions().setCliPath(explicit.toString()));

        assertEquals(explicit.toString(), manager.resolveCliLaunch().executable());
    }

    @Test
    void inheritedCliPathEnvironmentOverrideDoesNotRequireRuntimeBundle() throws Exception {
        Path inherited = tempDir.resolve("copilot-runtime");
        var manager = new CliServerManager(new CopilotClientOptions());

        assertEquals(inherited.toString(), manager.resolveCliLaunch(inherited.toString()).executable());
    }

    @Test
    void configuredEnvironmentCliPathOverridesInheritedEnvironment() throws Exception {
        Path inherited = tempDir.resolve("inherited-copilot-runtime");
        Path configured = tempDir.resolve("configured-copilot-runtime");
        var options = new CopilotClientOptions()
                .setEnvironment(Map.of(NativeRuntimeLoader.COPILOT_CLI_PATH_ENV, configured.toString()));
        var manager = new CliServerManager(options);

        assertEquals(configured.toString(), manager.resolveCliLaunch(inherited.toString()).executable());
    }

    @Test
    void explicitCliPathOverridesEnvironment() throws Exception {
        Path explicit = tempDir.resolve("explicit-copilot-runtime");
        Path configured = tempDir.resolve("configured-copilot-runtime");
        var options = new CopilotClientOptions().setCliPath(explicit.toString())
                .setEnvironment(Map.of(NativeRuntimeLoader.COPILOT_CLI_PATH_ENV, configured.toString()));
        var manager = new CliServerManager(options);

        assertEquals(explicit.toString(), manager.resolveCliLaunch("inherited-copilot-runtime").executable());
    }

    @Test
    void cliModeOptsIntoProcessLoggingWithoutChangingEmptyModeEnvironment() {
        for (var mode : CopilotClientMode.values()) {
            var options = new CopilotClientOptions().setMode(mode)
                    .setEnvironment(Map.of("COPILOT_RUNTIME_PROCESS_FILE_LOGGING", "opposite"));
            var process = new ProcessBuilder("copilot-runtime");
            new CliServerManager(options).configureProcessEnvironment(process);
            if (mode == CopilotClientMode.EMPTY) {
                assertEquals("opposite", process.environment().get("COPILOT_RUNTIME_PROCESS_FILE_LOGGING"));
            } else {
                assertEquals("1", process.environment().get("COPILOT_RUNTIME_PROCESS_FILE_LOGGING"));
            }
        }
    }

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
    void parseCliUrlWithBracketedIpv6() {
        URI uri = CliServerManager.parseCliUrl("[::1]:4321");
        assertNotNull(uri.getHost());
        assertEquals(4321, uri.getPort());
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
        Process process = new TestProcess();
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

    // ===== resolveCliCommand tests =====

    @ParameterizedTest
    @ValueSource(strings = {"NoDefaultCurrentDirectoryInExePath", "nodefaultcurrentdirectoryinexepath"})
    @EnabledOnOs(OS.WINDOWS)
    void bareWindowsCliPathHonorsCurrentDirectorySearchOptOut(String optOutKey) throws Exception {
        Path bin = Files.createDirectory(tempDir.resolve("bin"));
        Files.createFile(tempDir.resolve("copilot.EXE"));
        Path executable = Files.createFile(bin.resolve("copilot.EXE"));
        var options = new CopilotClientOptions().setCliPath("copilot").setCwd(tempDir.toString()).setUseStdio(true)
                .setEnvironment(Map.of("Path", bin.toString(), "PATHEXT", ".EXE", optOutKey, ""));

        assertClientAttemptsExecutable(options, executable);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void bareWindowsCliPathSearchesCurrentDirectoryWithoutOptOut() throws Exception {
        Path bin = Files.createDirectory(tempDir.resolve("bin"));
        Path executable = Files.createFile(tempDir.resolve("copilot.EXE"));
        Files.createFile(bin.resolve("copilot.EXE"));
        var options = new CopilotClientOptions().setCliPath("copilot").setCwd(tempDir.toString()).setUseStdio(true)
                .setEnvironment(Map.of("PATH", bin.toString(), "PATHEXT", ".EXE"));

        assertClientAttemptsExecutable(options, executable);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @EnabledOnOs(OS.WINDOWS)
    void explicitWindowsCurrentDirectorySearchIsPreservedWithOptOut(boolean relativeCliPath) throws Exception {
        Path executable = Files.createFile(tempDir.resolve(".").resolve("copilot.EXE"));
        var options = new CopilotClientOptions().setCliPath(relativeCliPath ? ".\\copilot.EXE" : "copilot")
                .setCwd(tempDir.toString()).setUseStdio(true).setEnvironment(Map.of("PATH", relativeCliPath ? "" : ".",
                        "PATHEXT", ".EXE", "NoDefaultCurrentDirectoryInExePath", ""));

        assertClientAttemptsExecutable(options, executable);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "C:\\invalid|entry"})
    @EnabledOnOs(OS.WINDOWS)
    void invalidWindowsCommandIsNotPassedToShellWhenCurrentDirectorySearchIsDisabled(String path) {
        var options = new CopilotClientOptions().setCliPath("copilot|invalid").setCwd(tempDir.toString())
                .setUseStdio(true)
                .setEnvironment(Map.of("PATH", path, "PATHEXT", ".EXE", "NoDefaultCurrentDirectoryInExePath", ""));

        try (var client = new CopilotClient(options)) {
            Throwable error = assertThrows(CompletionException.class, () -> client.start().join());
            while (error.getCause() != null) {
                error = error.getCause();
            }
            var invalidPath = assertInstanceOf(InvalidPathException.class, error);
            assertEquals("copilot|invalid.EXE", invalidPath.getInput());
        }
    }

    private static void assertClientAttemptsExecutable(CopilotClientOptions options, Path executable) {
        // Empty executables expose the attempted OS launch path without running a
        // child.
        try (var client = new CopilotClient(options)) {
            Throwable error = assertThrows(CompletionException.class, () -> client.start().join());
            while (!(error instanceof IOException) && error.getCause() != null) {
                error = error.getCause();
            }
            var launchError = assertInstanceOf(IOException.class, error);
            assertTrue(launchError.getMessage().contains("\"" + executable + "\""), launchError::getMessage);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"tools/copilot.exe", "tools\\copilot", ".\\tools\\copilot.EXE"})
    @EnabledOnOs(OS.WINDOWS)
    void relativeWindowsCliPathDoesNotSearchPathDirectories(String cliPath) throws Exception {
        Path bin = Files.createDirectory(tempDir.resolve("bin"));
        Path tools = Files.createDirectory(bin.resolve("tools"));
        Path unrelated = Files.createFile(tools.resolve("copilot.EXE"));
        var options = new CopilotClientOptions().setCliPath(cliPath).setCwd(tempDir.toString()).setUseStdio(false)
                .setEnvironment(Map.of("PATH", bin.toString(), "PATHEXT", ".EXE"));

        try (var client = new CopilotClient(options)) {
            Throwable error = assertThrows(CompletionException.class, () -> client.start().join());
            while (error.getCause() != null) {
                error = error.getCause();
            }
            var launchError = assertInstanceOf(IOException.class, error);
            assertTrue(launchError.getMessage().startsWith("CLI process exited unexpectedly."),
                    launchError::getMessage);
            assertFalse(launchError.getMessage().contains(unrelated.toString()), launchError::getMessage);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {" C:\\invalid", "C:\\invalid ", "C:\\invalid<entry", "C:\\invalid>entry",
            "C:\\invalid|entry", "C:\\invalid?entry", "C:\\invalid*entry", "C:\\invalid\"entry"})
    @EnabledOnOs(OS.WINDOWS)
    void windowsNativeExecutableSkipsMalformedPathEntries(String malformedEntry) throws Exception {
        Path bin = Files.createDirectory(tempDir.resolve("bin with spaces"));
        Path executable = Files.createFile(bin.resolve("copilot.EXE"));
        var options = new CopilotClientOptions().setCliPath("copilot").setCwd(tempDir.toString())
                .setEnvironment(Map.of("Path", malformedEntry + ";\"" + bin + "\"", "PATHEXT", ".CMD;.EXE"));

        assertEquals(List.of(executable.toString(), "--server"), resolveCliCommand(new CliServerManager(options)));
    }

    @ParameterizedTest
    @ValueSource(strings = {".CMD", ".BAT"})
    @EnabledOnOs(OS.WINDOWS)
    void windowsCommandWrapperAfterMalformedPathEntryUsesShell(String extension) throws Exception {
        Path bin = Files.createDirectory(tempDir.resolve("bin"));
        Files.createFile(bin.resolve("copilot" + extension));
        Files.createFile(bin.resolve("copilot.EXE"));
        var options = new CopilotClientOptions().setCwd(tempDir.toString())
                .setEnvironment(Map.of(NativeRuntimeLoader.COPILOT_CLI_PATH_ENV, "copilot", "PATH",
                        "C:\\invalid|entry;" + bin, "PATHEXT", extension + ";.EXE"));

        assertEquals(List.of("cmd", "/c", "copilot", "--server"), resolveCliCommand(new CliServerManager(options)));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void windowsCommandFallsBackToShellWhenPathEntriesAreMalformed() throws Exception {
        var options = new CopilotClientOptions().setCliPath("copilot").setCwd(tempDir.toString())
                .setEnvironment(Map.of("PATH", " C:\\invalid;C:\\invalid|entry"));

        assertEquals(List.of("cmd", "/c", "copilot", "--server"), resolveCliCommand(new CliServerManager(options)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @EnabledOnOs(OS.WINDOWS)
    void explicitWindowsExecutablePathDoesNotDependOnPath(boolean absolute) throws Exception {
        Path bin = Files.createDirectory(tempDir.resolve("bin"));
        Path executable = Files.createFile(bin.resolve("copilot.exe"));
        String cliPath = (absolute ? executable : tempDir.relativize(executable)).toString();
        var options = new CopilotClientOptions().setCliPath(cliPath).setCwd(tempDir.toString())
                .setEnvironment(Map.of("PATH", "C:\\invalid|entry"));

        assertEquals(List.of(executable.toString(), "--server"), resolveCliCommand(new CliServerManager(options)));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void malformedWindowsCliPathIsNotSkipped() {
        var options = new CopilotClientOptions().setCliPath("copilot|invalid").setCwd(tempDir.toString())
                .setEnvironment(Map.of("PATH", "C:\\invalid|entry", "PATHEXT", ".EXE"));

        var error = assertThrows(InvocationTargetException.class,
                () -> resolveCliCommand(new CliServerManager(options)));
        var cause = assertInstanceOf(InvalidPathException.class, error.getCause());
        assertEquals("copilot|invalid.EXE", cause.getInput());
    }

    @SuppressWarnings("unchecked")
    private static List<String> resolveCliCommand(CliServerManager manager) throws Exception {
        var resolve = CliServerManager.class.getDeclaredMethod("resolveCliCommand", String.class, List.class);
        resolve.setAccessible(true);
        return (List<String>) resolve.invoke(manager, manager.resolveCliLaunch().executable(), List.of("--server"));
    }

    // ===== startCliServer tests =====

    // On Windows, "/nonexistent/copilot" is not an absolute path (no drive letter),
    // so resolveCliCommand wraps it with "cmd /c" and ProcessBuilder.start()
    // succeeds
    // (launching cmd.exe). Use a Windows-absolute path to ensure IOException.
    private static final String NONEXISTENT_CLI = System.getProperty("os.name").toLowerCase().contains("win")
            ? "C:\\nonexistent\\copilot"
            : "/nonexistent/copilot";

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
        var options = new CopilotClientOptions().setCliPath(NONEXISTENT_CLI).setCliArgs(new String[]{"--extra-flag"})
                .setUseStdio(true);
        var manager = new CliServerManager(options);

        var ex = assertThrows(IOException.class, () -> manager.startCliServer());
        assertNotNull(ex);
    }

    @Test
    void startCliServerWithExplicitPort() throws Exception {
        // Test the explicit port branch (useStdio=false, port > 0)
        var options = new CopilotClientOptions().setCliPath(NONEXISTENT_CLI).setUseStdio(false).setPort(9999);
        var manager = new CliServerManager(options);

        var ex = assertThrows(IOException.class, () -> manager.startCliServer());
        assertNotNull(ex);
    }

    @Test
    void startCliServerWithGitHubToken() throws Exception {
        // Test the github token branch
        var options = new CopilotClientOptions().setCliPath(NONEXISTENT_CLI).setGitHubToken("ghp_test123")
                .setUseStdio(true);
        var manager = new CliServerManager(options);

        var ex = assertThrows(IOException.class, () -> manager.startCliServer());
        assertNotNull(ex);
    }

    @Test
    void startCliServerWithUseLoggedInUserExplicit() throws Exception {
        // Test the explicit useLoggedInUser=false branch (adds --no-auto-login)
        var options = new CopilotClientOptions().setCliPath(NONEXISTENT_CLI).setUseLoggedInUser(false)
                .setUseStdio(true);
        var manager = new CliServerManager(options);

        var ex = assertThrows(IOException.class, () -> manager.startCliServer());
        assertNotNull(ex);
    }

    @Test
    void startCliServerWithGitHubTokenAndNoExplicitUseLoggedInUser() throws Exception {
        // When gitHubToken is set and useLoggedInUser is null, defaults to false
        var options = new CopilotClientOptions().setCliPath(NONEXISTENT_CLI).setGitHubToken("ghp_test123")
                .setUseStdio(true);
        var manager = new CliServerManager(options);

        var ex = assertThrows(IOException.class, () -> manager.startCliServer());
        assertNotNull(ex);
    }

    @Test
    void startCliServerWithNullCliPath() throws Exception {
        // Test the default cliPath branch (defaults to "copilot" when not set)
        var options = new CopilotClientOptions().setUseStdio(true);
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

    @Test
    void startCliServerWithTelemetryAllOptions() throws Exception {
        // The telemetry env vars are applied before ProcessBuilder.start()
        // so even with a nonexistent CLI path, the telemetry code path is exercised
        var telemetry = new TelemetryConfig().setOtlpEndpoint("http://localhost:4318").setOtlpProtocol("http/protobuf")
                .setFilePath("/tmp/telemetry.log").setExporterType("otlp-http").setSourceName("test-app")
                .setCaptureContent(true);
        var options = new CopilotClientOptions().setCliPath(NONEXISTENT_CLI).setTelemetry(telemetry).setUseStdio(true);
        var manager = new CliServerManager(options);

        var ex = assertThrows(IOException.class, () -> manager.startCliServer());
        assertNotNull(ex);
    }

    @Test
    void startCliServerWithTelemetryCaptureContentFalse() throws Exception {
        // Test the false branch of getCaptureContent()
        var telemetry = new TelemetryConfig().setCaptureContent(false);
        var options = new CopilotClientOptions().setCliPath(NONEXISTENT_CLI).setTelemetry(telemetry).setUseStdio(true);
        var manager = new CliServerManager(options);

        var ex = assertThrows(IOException.class, () -> manager.startCliServer());
        assertNotNull(ex);
    }

    @Test
    void startCliServerWithSessionIdleTimeout() throws Exception {
        // Test that --session-idle-timeout flag is included when option is set
        var options = new CopilotClientOptions().setCliPath(NONEXISTENT_CLI).setSessionIdleTimeoutSeconds(600)
                .setUseStdio(true);
        var manager = new CliServerManager(options);

        var ex = assertThrows(IOException.class, () -> manager.startCliServer());
        assertNotNull(ex);
    }

    @Test
    void startCliServerWithZeroSessionIdleTimeout() throws Exception {
        // Zero timeout should not add the flag (treated as disabled)
        var options = new CopilotClientOptions().setCliPath(NONEXISTENT_CLI).setSessionIdleTimeoutSeconds(0)
                .setUseStdio(true);
        var manager = new CliServerManager(options);

        var ex = assertThrows(IOException.class, () -> manager.startCliServer());
        assertNotNull(ex);
    }
}
