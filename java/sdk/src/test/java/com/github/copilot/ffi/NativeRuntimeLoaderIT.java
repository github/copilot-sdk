/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Exercises public resolution in owned JVMs with the real classifier JAR
 * supplied by {@code -Pinprocess}. Resolves paths without loading or executing
 * native code.
 */
@EnabledIfEnvironmentVariable(named = "COPILOT_SDK_DEFAULT_CONNECTION", matches = "inprocess")
class NativeRuntimeLoaderIT {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void incompatibleExplicitPackageDoesNotFallBack(boolean warmCache, @TempDir Path tempDir) throws Exception {
        Path controlHome = Files.createDirectory(tempDir.resolve("control-home"));
        Path cachedRuntime = resolvedPath(runResolve(null, controlHome));
        assertTrue(cachedRuntime.startsWith(controlHome.resolve(".copilot/runtime-cache")), cachedRuntime.toString());
        assertTrue(Files.size(cachedRuntime) > 0);
        assertEquals(cachedRuntime, resolvedPath(runResolve("   ", controlHome)));

        Path home = warmCache ? controlHome : Files.createDirectory(tempDir.resolve("cold-home"));
        Path cli = Files.writeString(tempDir.resolve("copilot"), "explicit CLI package");
        assertInvalidOverride(cli, home, "runtime.node");

        Files.createFile(tempDir.resolve("runtime.node"));
        Path prebuilds = Files
                .createDirectories(tempDir.resolve("prebuilds").resolve(PlatformDetector.detectClassifier()));
        Files.createFile(prebuilds.resolve("runtime.node"));
        assertInvalidOverride(cli, home, "runtime.node");

        if (!warmCache) {
            assertFalse(Files.exists(home.resolve(".copilot/runtime-cache")));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void validExplicitPackageTakesPrecedence(boolean prebuilds, @TempDir Path tempDir) throws Exception {
        Path home = Files.createDirectory(tempDir.resolve("home"));
        Path cli = Files.writeString(tempDir.resolve("copilot"), "explicit CLI package");
        Path runtimeDirectory = prebuilds
                ? Files.createDirectories(tempDir.resolve("prebuilds").resolve(PlatformDetector.detectClassifier()))
                : tempDir;
        Path runtime = runtimeDirectory.resolve("runtime.node");
        try (var source = runtimeResource().openStream()) {
            Files.copy(source, runtime);
        }

        assertEquals(runtime, resolvedPath(runResolve(cli.toString(), home)));
        assertFalse(Files.exists(home.resolve(".copilot/runtime-cache")));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void invalidEntrypointDoesNotUseAdjacentRuntime(boolean emptyFile, @TempDir Path tempDir) throws Exception {
        Path home = Files.createDirectory(tempDir.resolve("home"));
        Path cli = tempDir.resolve("copilot");
        if (emptyFile) {
            Files.createFile(cli);
        }
        try (var source = runtimeResource().openStream()) {
            Files.copy(source, tempDir.resolve("runtime.node"));
        }

        assertInvalidOverride(cli, home, "CLI entrypoint");
        assertFalse(Files.exists(home.resolve(".copilot/runtime-cache")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"configured", "target", "both"})
    @DisabledOnOs(OS.WINDOWS)
    void explicitCliSymlinkUsesSelectedInstallation(String libraryLocation, @TempDir Path tempDir) throws Exception {
        tempDir = tempDir.toRealPath();
        Path home = Files.createDirectory(tempDir.resolve("home"));
        Path installation = Files.createDirectory(tempDir.resolve("installation"));
        Path cli = Files.writeString(installation.resolve("copilot"), "explicit CLI");
        Path link = Files.createSymbolicLink(tempDir.resolve("copilot"), cli);
        Path runtime = (libraryLocation.equals("target") ? installation : tempDir).resolve("runtime.node");
        try (var source = runtimeResource().openStream()) {
            Files.copy(source, runtime);
        }
        if (libraryLocation.equals("both")) {
            Files.copy(runtime, installation.resolve("runtime.node"));
        }

        ProbeResult result = runResolve(link.toString(), home);
        assertEquals(runtime.toRealPath(), resolvedPath(result));
        assertTrue(result.output().contains("entrypoint=" + link), result.output());
        assertFalse(Files.exists(home.resolve(".copilot/runtime-cache")));
    }

    private static void assertInvalidOverride(Path cli, Path home, String artifact) throws Exception {
        ProbeResult result = runResolve(cli.toString(), home);
        assertNotEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("java.io.FileNotFoundException"), result.output());
        assertTrue(result.output().contains("COPILOT_CLI_PATH"), result.output());
        assertTrue(result.output().contains(cli.toString()), result.output());
        assertTrue(result.output().contains(artifact), result.output());
    }

    private static Path resolvedPath(ProbeResult result) {
        assertEquals(0, result.exitCode(), result.output());
        return Path.of(result.output().lines().filter(line -> line.startsWith("resolved=")).findFirst()
                .orElseThrow(() -> new AssertionError(result.output())).substring("resolved=".length()));
    }

    private static URL runtimeResource() {
        String resource = "native/" + PlatformDetector.detectClassifier() + "/runtime.node";
        URL url = NativeRuntimeLoader.class.getClassLoader().getResource(resource);
        assertNotNull(url, "The inprocess profile must supply " + resource);
        assertEquals("jar", url.getProtocol(), "Use the packaged classifier artifact, not synthetic resources");
        return url;
    }

    private static ProbeResult runResolve(String cliPath, Path home) throws Exception {
        Path java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");
        Path sdkJar = Path.of(System.getProperty("project.build.directory"),
                System.getProperty("project.build.finalName") + ".jar");
        Path nativeJar = Path.of(((JarURLConnection) runtimeResource().openConnection()).getJarFileURL().toURI());
        String classpath = String.join(File.pathSeparator, sdkJar.toString(),
                System.getProperty("project.build.testOutputDirectory"), nativeJar.toString());
        Path log = Files.createTempFile(home, "resolve-", ".log");
        ProcessBuilder builder = new ProcessBuilder(java.toString(), "-Duser.home=" + home, "-cp", classpath,
                ResolveProbe.class.getName()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("PATH", "");
        if (cliPath == null) {
            builder.environment().remove("COPILOT_CLI_PATH");
        } else {
            builder.environment().put("COPILOT_CLI_PATH", cliPath);
        }
        Process process = builder.start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Resolver JVM did not exit:\n" + Files.readString(log));
            return new ProbeResult(process.exitValue(), Files.readString(log));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor();
            }
        }
    }

    private record ProbeResult(int exitCode, String output) {
    }

    public static final class ResolveProbe {
        public static void main(String[] args) throws Exception {
            System.out.println("resolved=" + NativeRuntimeLoader.resolve());
            System.out.println("entrypoint=" + NativeRuntimeLoader.resolveConfiguredEntrypoint());
        }
    }
}
