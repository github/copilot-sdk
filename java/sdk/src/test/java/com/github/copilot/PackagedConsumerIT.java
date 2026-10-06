/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackagedConsumerIT {

    @Test
    void runsStandaloneConsumerWithManifestClasspath(@TempDir Path directory) throws Exception {
        Path build = Path.of(System.getProperty("project.build.directory"));
        Path library = Files.createDirectory(directory.resolve("lib"));
        var dependencies = new ArrayList<Path>();
        dependencies.add(build.resolve(System.getProperty("project.build.finalName") + ".jar"));
        try (var jars = Files.list(build.resolve("consumer-dependencies"))) {
            dependencies.addAll(jars.filter(path -> path.toString().endsWith(".jar")).sorted().toList());
        }
        for (Path dependency : dependencies) {
            Files.copy(dependency, library.resolve(dependency.getFileName()));
        }

        Path source = directory.resolve("Consumer.java");
        Files.writeString(source, """
                import com.github.copilot.CopilotClient;
                import com.github.copilot.ConnectionState;
                import com.github.copilot.rpc.MessageOptions;
                import com.fasterxml.jackson.databind.ObjectMapper;
                import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

                public class Consumer {
                    public static void main(String[] args) throws Exception {
                        try (var client = new CopilotClient()) {
                            var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
                            var json = mapper.writeValueAsString(new MessageOptions().setPrompt("consumer"));
                            if (!mapper.readTree(json).get("prompt").asText().equals("consumer")
                                    || client.getState() != ConnectionState.DISCONNECTED) {
                                throw new AssertionError("Packaged SDK consumer failed");
                            }
                        }
                        System.out.println("consumer-ok");
                    }
                }
                """);

        String classpath = String.join(File.pathSeparator,
                dependencies.stream().map(path -> library.resolve(path.getFileName()).toString()).toList());
        var diagnostics = new ByteArrayOutputStream();
        int compilation = ToolProvider.getSystemJavaCompiler().run(null, diagnostics, diagnostics, "--release", "17",
                "-proc:none", "-classpath", classpath, source.toString());
        assertEquals(0, compilation, diagnostics.toString(StandardCharsets.UTF_8));

        var manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "Consumer");
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH,
                String.join(" ", dependencies.stream().map(path -> "lib/" + path.getFileName()).toList()));
        Path consumerJar = directory.resolve("consumer.jar");
        try (var jar = new JarOutputStream(Files.newOutputStream(consumerJar), manifest)) {
            jar.putNextEntry(new JarEntry("Consumer.class"));
            Files.copy(directory.resolve("Consumer.class"), jar);
            jar.closeEntry();
        }
        Files.delete(directory.resolve("Consumer.class"));

        Path java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
        Path output = directory.resolve("output.log");
        ProcessBuilder builder = new ProcessBuilder(java.toString(), "-jar", consumerJar.toString())
                .directory(directory.toFile()).redirectErrorStream(true).redirectOutput(output.toFile());
        builder.environment().remove("COPILOT_SDK_DEFAULT_CONNECTION");
        Process process = builder.start();
        try {
            boolean exited = process.waitFor(30, TimeUnit.SECONDS);
            String text = Files.readString(output);
            assertTrue(exited, "Consumer JVM did not exit. Output:\n" + text);
            assertEquals(0, process.exitValue(), text);
            assertTrue(text.contains("consumer-ok"), text);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly().waitFor(30, TimeUnit.SECONDS);
            }
        }
    }
}
