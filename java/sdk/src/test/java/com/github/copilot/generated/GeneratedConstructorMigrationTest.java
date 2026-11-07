/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("java-compatibility")
class GeneratedConstructorMigrationTest {
    @TempDir
    Path tempDirectory;

    @Test
    void handwrittenProductionUsesNamedConstructionExceptApprovedCompatibilitySites() throws Exception {
        Path moduleRoot = Path
                .of(GeneratedConstructorMigrationTest.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .getParent().getParent();
        Path helper = moduleRoot.resolve("src/test/resources/compiler-guard/GeneratedConstructorMigrationGuard.java");
        assertTrue(Files.isRegularFile(helper), "Missing resolved-symbol guard helper: " + helper);

        Path classes = Files.createDirectory(tempDirectory.resolve("guard-classes"));
        String classpath = compilerClasspath(moduleRoot);
        String javac = javaTool("javac");
        String java = javaTool("java");
        run(List.of(javac, "--add-modules", "jdk.compiler", "-classpath", classpath, "-d", classes.toString(),
                helper.toString()));
        run(List.of(java, "--add-modules", "jdk.compiler", "-cp", classes.toString(),
                "GeneratedConstructorMigrationGuard", moduleRoot.toString(), classpath));
    }

    private static String javaTool(String name) {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        return Path.of(System.getProperty("java.home"), "bin", name + (windows ? ".exe" : "")).toString();
    }

    private static void run(List<String> command) throws Exception {
        Process process = new ProcessBuilder(new ArrayList<>(command)).redirectErrorStream(true).start();
        String output;
        try (InputStream stream = process.getInputStream()) {
            output = new String(stream.readAllBytes());
        }
        assertEquals(0, process.waitFor(), () -> String.join(" ", command) + "\n" + output);
    }

    private static String compilerClasspath(Path moduleRoot) {
        Set<Path> entries = new LinkedHashSet<>();
        entries.add(moduleRoot.resolve("target/classes"));
        entries.add(moduleRoot.resolve("target/test-classes"));
        for (String entry : System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"))
                .split(java.util.regex.Pattern.quote(System.getProperty("path.separator")))) {
            if (!entry.isEmpty()) {
                entries.add(Path.of(entry));
            }
        }
        ModuleLayer.boot().configuration().modules().stream().map(resolved -> resolved.reference().location())
                .flatMap(java.util.Optional::stream).filter(uri -> uri.getScheme().equals("file")).map(Path::of)
                .filter(Files::exists).forEach(entries::add);
        return String.join(System.getProperty("path.separator"), entries.stream().map(Path::toString).toList());
    }
}
