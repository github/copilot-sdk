/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Failsafe integration test that asserts the multi-release behaviour of
 * {@link InternalExecutorProvider} against the actually packaged JAR.
 * <p>
 * Runs after {@code package}, when {@code target/${finalName}.jar} exists with
 * its real {@code Multi-Release: true} manifest and (on JDK 25+ builds) the
 * {@code META-INF/versions/25/} override produced by {@code maven-jar-plugin}.
 * <p>
 * The test spawns a child JVM with the packaged JAR plus {@code test-classes}
 * on the classpath, runs {@link InternalExecutorProviderProbe}, and asserts
 * that the executor selected for the current runtime matches expectations.
 */
class InternalExecutorProviderIT {

    @Test
    void packagedJarSelectsExecutorPerRuntimeVersion() throws Exception {
        Path packagedJar = locatePackagedJar();
        Path testClasses = locateTestClassesDir();
        String javaBin = locateJavaBinary();

        String classpath = packagedJar.toString() + File.pathSeparator + testClasses.toString();
        Process process = new ProcessBuilder(javaBin, "-cp", classpath,
                "com.github.copilot.InternalExecutorProviderProbe").redirectErrorStream(true).start();

        String output;
        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Probe JVM did not exit within 30s. Output:\n" + output);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }

        assertEquals(0, process.exitValue(), "Probe exited non-zero. Output:\n" + output);

        Map<String, String> kv = parseKeyValues(output);
        String featureRaw = kv.get("feature");
        assertNotNull(featureRaw, "Probe did not report 'feature'. Output:\n" + output);
        int feature = Integer.parseInt(featureRaw);

        boolean expectOwnedVirtual = feature >= 25;
        assertEquals(String.valueOf(expectOwnedVirtual), kv.get("canBeShutdown"),
                "canBeShutdown mismatch for JDK feature=" + feature + ". Output:\n" + output);
        assertEquals(String.valueOf(expectOwnedVirtual), kv.get("virtual"),
                "virtual mismatch for JDK feature=" + feature + ". Output:\n" + output);
    }

    private static Path locatePackagedJar() {
        String buildDir = System.getProperty("project.build.directory");
        String finalName = System.getProperty("project.build.finalName");
        assertNotNull(buildDir, "System property 'project.build.directory' must be set by failsafe");
        assertNotNull(finalName, "System property 'project.build.finalName' must be set by failsafe");
        Path jar = Path.of(buildDir, finalName + ".jar");
        assertTrue(Files.isRegularFile(jar), "Packaged JAR must exist: " + jar);
        return jar;
    }

    private static Path locateTestClassesDir() {
        String testOutput = System.getProperty("project.build.testOutputDirectory");
        assertNotNull(testOutput, "System property 'project.build.testOutputDirectory' must be set by failsafe");
        Path dir = Path.of(testOutput);
        assertTrue(Files.isDirectory(dir), "test-classes dir must exist: " + dir);
        return dir;
    }

    private static String locateJavaBinary() {
        Path javaHome = Path.of(System.getProperty("java.home"));
        Path candidate = javaHome.resolve("bin").resolve(isWindows() ? "java.exe" : "java");
        assertTrue(Files.isExecutable(candidate), "java binary must be executable: " + candidate);
        return candidate.toString();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static Map<String, String> parseKeyValues(String output) {
        Map<String, String> map = new HashMap<>();
        for (String line : output.split("\\R")) {
            int eq = line.indexOf('=');
            if (eq > 0) {
                map.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        }
        return map;
    }
}
