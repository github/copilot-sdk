/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests that {@link CopilotToolProcessor} correctly generates
 * {@code $$CopilotToolMeta} companion classes and emits compile errors for
 * invalid usages.
 */
class CopilotToolProcessorTest {

    @TempDir
    java.nio.file.Path tempDir;

    // ── Test: Basic generation ──────────────────────────────────────────────────

    @Test
    void generatesMetaClass_withCorrectToolNames() {
        String source = """
                package test;
                import com.github.copilot.tool.CopilotTool;
                import com.github.copilot.tool.Param;
                public class MyTools {
                    @CopilotTool("Sets the current phase")
                    public String setCurrentPhase(@Param("The phase") String phase) {
                        return "done";
                    }
                    @CopilotTool("Search for items")
                    public String searchItems(@Param("Keyword") String keyword) {
                        return "found";
                    }
                    @CopilotTool(value = "Custom grep", name = "grep")
                    public String grepOverride(@Param("Query") String query) {
                        return "result";
                    }
                }
                """;

        CompilationResult result = compileWithProcessor(List.of(inMemorySource("test.MyTools", source)));

        assertNoErrors(result);
        // Verify generated source contains the expected tool names
        String generated = result.getGeneratedSource("test.MyTools$$CopilotToolMeta");
        assertTrue(generated != null, "Expected $$CopilotToolMeta to be generated");
        assertTrue(generated.contains("\"set_current_phase\""), "Expected snake_case name: set_current_phase");
        assertTrue(generated.contains("\"search_items\""), "Expected snake_case name: search_items");
        assertTrue(generated.contains("\"grep\""), "Expected explicit name: grep");
    }

    // ── Test: Compile error for private methods ─────────────────────────────────

    @Test
    void emitsError_forPrivateMethods() {
        String source = """
                package test;
                import com.github.copilot.tool.CopilotTool;
                public class PrivateTools {
                    @CopilotTool("Private tool")
                    private String doSomething() {
                        return "done";
                    }
                }
                """;

        CompilationResult result = compileWithProcessor(List.of(inMemorySource("test.PrivateTools", source)));

        assertTrue(hasErrorContaining(result, "must not be private"),
                "Expected compile error for private @CopilotTool method, got: " + result.diagnostics);
    }

    // ── Test: Compile error for required + defaultValue conflict ─────────────

    @Test
    void emitsError_forRequiredWithDefaultValue() {
        String source = """
                package test;
                import com.github.copilot.tool.CopilotTool;
                import com.github.copilot.tool.Param;
                public class ConflictTools {
                    @CopilotTool("Conflicting params")
                    public String doSomething(@Param(value = "desc", required = true, defaultValue = "hello") String param) {
                        return "done";
                    }
                }
                """;

        CompilationResult result = compileWithProcessor(List.of(inMemorySource("test.ConflictTools", source)));

        assertTrue(hasErrorContaining(result, "required=true"),
                "Expected compile error for required+defaultValue conflict, got: " + result.diagnostics);
    }

    // ── Test: Return type handling ──────────────────────────────────────────────

    @Test
    void generatesCorrectCode_forStringReturnType() {
        String source = """
                package test;
                import com.github.copilot.tool.CopilotTool;
                import com.github.copilot.tool.Param;
                public class StringReturn {
                    @CopilotTool("Returns string")
                    public String doSomething(@Param("Input") String input) {
                        return input;
                    }
                }
                """;

        CompilationResult result = compileWithProcessor(List.of(inMemorySource("test.StringReturn", source)));
        assertNoErrors(result);
        String generated = result.getGeneratedSource("test.StringReturn$$CopilotToolMeta");
        assertTrue(generated.contains("CompletableFuture.completedFuture(instance.doSomething("),
                "Expected completedFuture wrapping for String return, got:\n" + generated);
    }

    @Test
    void generatesCorrectCode_forVoidReturnType() {
        String source = """
                package test;
                import com.github.copilot.tool.CopilotTool;
                import com.github.copilot.tool.Param;
                public class VoidReturn {
                    @CopilotTool("Void method")
                    public void doSomething(@Param("Input") String input) {
                    }
                }
                """;

        CompilationResult result = compileWithProcessor(List.of(inMemorySource("test.VoidReturn", source)));
        assertNoErrors(result);
        String generated = result.getGeneratedSource("test.VoidReturn$$CopilotToolMeta");
        assertTrue(generated.contains("instance.doSomething("), "Expected method call in generated code");
        assertTrue(generated.contains("CompletableFuture.completedFuture(\"Success\")"),
                "Expected 'Success' return for void methods, got:\n" + generated);
    }

    @Test
    void generatesCorrectCode_forCompletableFutureStringReturnType() {
        String source = """
                package test;
                import com.github.copilot.tool.CopilotTool;
                import com.github.copilot.tool.Param;
                import java.util.concurrent.CompletableFuture;
                public class AsyncReturn {
                    @CopilotTool("Async method")
                    public CompletableFuture<String> doSomething(@Param("Input") String input) {
                        return CompletableFuture.completedFuture(input);
                    }
                }
                """;

        CompilationResult result = compileWithProcessor(List.of(inMemorySource("test.AsyncReturn", source)));
        assertNoErrors(result);
        String generated = result.getGeneratedSource("test.AsyncReturn$$CopilotToolMeta");
        assertTrue(generated.contains("return instance.doSomething("),
                "Expected direct return for CompletableFuture<String>, got:\n" + generated);
        assertTrue(generated.contains("thenApply(r -> (Object) r)"),
                "Expected thenApply cast for CompletableFuture<String>, got:\n" + generated);
    }

    @Test
    void generatesCorrectCode_forIntReturnType() {
        String source = """
                package test;
                import com.github.copilot.tool.CopilotTool;
                import com.github.copilot.tool.Param;
                public class IntReturn {
                    @CopilotTool("Returns int")
                    public int doSomething(@Param("Input") String input) {
                        return 42;
                    }
                }
                """;

        CompilationResult result = compileWithProcessor(List.of(inMemorySource("test.IntReturn", source)));
        assertNoErrors(result);
        String generated = result.getGeneratedSource("test.IntReturn$$CopilotToolMeta");
        assertTrue(generated.contains("mapper.writeValueAsString(instance.doSomething("),
                "Expected JSON serialization for int return type, got:\n" + generated);
    }

    // ── Test: Argument coercion ─────────────────────────────────────────────────

    @Test
    void generatesCorrectArgExtraction_forPrimitiveAndStringTypes() {
        String source = """
                package test;
                import com.github.copilot.tool.CopilotTool;
                import com.github.copilot.tool.Param;
                public class ArgTypes {
                    @CopilotTool("Mixed args")
                    public String doSomething(
                            @Param("Name") String name,
                            @Param("Count") int count,
                            @Param("Flag") boolean flag) {
                        return "done";
                    }
                }
                """;

        CompilationResult result = compileWithProcessor(List.of(inMemorySource("test.ArgTypes", source)));
        assertNoErrors(result);
        String generated = result.getGeneratedSource("test.ArgTypes$$CopilotToolMeta");
        assertTrue(generated.contains("(String) args.get(\"name\")"),
                "Expected String cast for String param, got:\n" + generated);
        assertTrue(generated.contains("((Number) args.get(\"count\")).intValue()"),
                "Expected Number cast for int param, got:\n" + generated);
        assertTrue(generated.contains("(Boolean) args.get(\"flag\")"),
                "Expected Boolean cast for boolean param, got:\n" + generated);
    }

    // ── Test: snake_case conversion ─────────────────────────────────────────────

    @Test
    void snakeCaseConversion() {
        assertEquals("set_current_phase", CopilotToolProcessor.toSnakeCase("setCurrentPhase"));
        assertEquals("search_items", CopilotToolProcessor.toSnakeCase("searchItems"));
        assertEquals("grep", CopilotToolProcessor.toSnakeCase("grep"));
        assertEquals("get_u_r_l", CopilotToolProcessor.toSnakeCase("getURL"));
        assertEquals("a", CopilotToolProcessor.toSnakeCase("a"));
        assertEquals("", CopilotToolProcessor.toSnakeCase(""));
    }

    // ── Test: Processor registration ────────────────────────────────────────────

    @Test
    void processorIsRegisteredInMetaInfServices() throws Exception {
        var resource = getClass().getClassLoader()
                .getResource("META-INF/services/javax.annotation.processing.Processor");
        assertTrue(resource != null, "META-INF/services/javax.annotation.processing.Processor should exist");
        String content = new String(resource.openStream().readAllBytes());
        assertTrue(content.contains("com.github.copilot.tool.CopilotToolProcessor"),
                "Service file should contain CopilotToolProcessor");
    }

    // ── Test: Schema generation in generated code ───────────────────────────────

    @Test
    void generatesCorrectSchema() {
        String source = """
                package test;
                import com.github.copilot.tool.CopilotTool;
                import com.github.copilot.tool.Param;
                public class SchemaTools {
                    @CopilotTool("Search items")
                    public String search(
                            @Param(value = "Query", required = true) String query,
                            @Param(value = "Limit", required = false) int limit) {
                        return "done";
                    }
                }
                """;

        CompilationResult result = compileWithProcessor(List.of(inMemorySource("test.SchemaTools", source)));
        assertNoErrors(result);
        String generated = result.getGeneratedSource("test.SchemaTools$$CopilotToolMeta");
        // Verify the schema contains the expected keys
        assertTrue(generated.contains("\"type\", \"object\""), "Expected object type in schema");
        assertTrue(generated.contains("\"properties\""), "Expected properties in schema");
        assertTrue(generated.contains("\"required\""), "Expected required in schema");
        assertTrue(generated.contains("\"query\""), "Expected query property");
    }

    // ── Test: package-private methods are allowed ───────────────────────────────

    @Test
    void allowsPackagePrivateMethods() {
        String source = """
                package test;
                import com.github.copilot.tool.CopilotTool;
                public class PackagePrivateTools {
                    @CopilotTool("Package private tool")
                    String doSomething() {
                        return "done";
                    }
                }
                """;

        CompilationResult result = compileWithProcessor(List.of(inMemorySource("test.PackagePrivateTools", source)));
        assertNoErrors(result);
    }

    // ── Test: protected methods are allowed ─────────────────────────────────────

    @Test
    void allowsProtectedMethods() {
        String source = """
                package test;
                import com.github.copilot.tool.CopilotTool;
                public class ProtectedTools {
                    @CopilotTool("Protected tool")
                    protected String doSomething() {
                        return "done";
                    }
                }
                """;

        CompilationResult result = compileWithProcessor(List.of(inMemorySource("test.ProtectedTools", source)));
        assertNoErrors(result);
    }

    // ── Test: overridesBuiltInTool generates createOverride ─────────────────────

    @Test
    void generatesCreateOverride_whenOverridesBuiltInTool() {
        String source = """
                package test;
                import com.github.copilot.tool.CopilotTool;
                import com.github.copilot.tool.Param;
                public class OverrideTools {
                    @CopilotTool(value = "Custom grep", name = "grep", overridesBuiltInTool = true)
                    public String grep(@Param("Query") String query) {
                        return "result";
                    }
                }
                """;

        CompilationResult result = compileWithProcessor(List.of(inMemorySource("test.OverrideTools", source)));
        assertNoErrors(result);
        String generated = result.getGeneratedSource("test.OverrideTools$$CopilotToolMeta");
        assertTrue(generated.contains("ToolDefinition.createOverride("),
                "Expected createOverride factory method, got:\n" + generated);
    }

    // ── Test: ToolDefer.NONE results in regular create ──────────────────────────

    @Test
    void generatesCreate_whenDeferIsNone() {
        String source = """
                package test;
                import com.github.copilot.tool.CopilotTool;
                import com.github.copilot.rpc.ToolDefer;
                public class DeferNoneTools {
                    @CopilotTool(value = "Simple tool", defer = ToolDefer.NONE)
                    public String doSomething() {
                        return "done";
                    }
                }
                """;

        CompilationResult result = compileWithProcessor(List.of(inMemorySource("test.DeferNoneTools", source)));
        assertNoErrors(result);
        String generated = result.getGeneratedSource("test.DeferNoneTools$$CopilotToolMeta");
        assertTrue(generated.contains("ToolDefinition.create("),
                "Expected create (not createWithDefer) for NONE, got:\n" + generated);
        assertFalse(generated.contains("createWithDefer"),
                "Should NOT use createWithDefer for NONE, got:\n" + generated);
    }

    // ── Test: ToolDefer.AUTO results in createWithDefer ──────────────────────────

    @Test
    void generatesCreateWithDefer_whenDeferIsAuto() {
        String source = """
                package test;
                import com.github.copilot.tool.CopilotTool;
                import com.github.copilot.rpc.ToolDefer;
                public class DeferAutoTools {
                    @CopilotTool(value = "Deferrable tool", defer = ToolDefer.AUTO)
                    public String doSomething() {
                        return "done";
                    }
                }
                """;

        CompilationResult result = compileWithProcessor(List.of(inMemorySource("test.DeferAutoTools", source)));
        assertNoErrors(result);
        String generated = result.getGeneratedSource("test.DeferAutoTools$$CopilotToolMeta");
        assertTrue(generated.contains("ToolDefinition.createWithDefer("),
                "Expected createWithDefer for AUTO, got:\n" + generated);
        assertTrue(generated.contains("ToolDefer.AUTO"), "Expected ToolDefer.AUTO argument, got:\n" + generated);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private CompilationResult compileWithProcessor(List<JavaFileObject> sources) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        String classpath = resolveClasspath();
        List<String> options = new ArrayList<>();
        options.addAll(List.of("-classpath", classpath));
        options.addAll(List.of("-d", tempDir.toString()));
        options.addAll(List.of("-s", tempDir.toString()));
        // Allow experimental APIs during test compilation
        options.add("-Acopilot.experimental.allowed=true");

        try {
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
            fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, List.of(tempDir.toFile()));
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(tempDir.toFile()));

            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null,
                    sources);
            task.setProcessors(List.of(new CopilotToolProcessor()));
            task.call();

            // Collect generated sources
            List<String> generatedSources = new ArrayList<>();
            collectGeneratedFiles(tempDir, generatedSources);

            return new CompilationResult(diagnostics.getDiagnostics(), generatedSources, tempDir);
        } catch (Exception e) {
            throw new RuntimeException("Compilation setup failed", e);
        }
    }

    private void collectGeneratedFiles(java.nio.file.Path dir, List<String> files) {
        try (var stream = java.nio.file.Files.walk(dir)) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    files.add(java.nio.file.Files.readString(p));
                } catch (java.io.IOException e) {
                    // ignore read errors for generated file collection
                }
            });
        } catch (java.io.IOException e) {
            // ignore walk errors
        }
    }

    private static String resolveClasspath() {
        // Collect classpath entries from CodeSource of key classes needed for
        // compiling both the source and the generated $$CopilotToolMeta code.
        Set<String> paths = new LinkedHashSet<>();

        // Add system classpath entries (may include manifest-only jars)
        String systemCp = System.getProperty("java.class.path", "");
        if (!systemCp.isEmpty()) {
            for (String p : systemCp.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                if (!p.isEmpty()) {
                    paths.add(p);
                }
            }
        }

        // Also resolve CodeSource paths for key classes (SDK + Jackson + RPC types)
        Class<?>[] keyClasses = {CopilotTool.class, com.fasterxml.jackson.databind.ObjectMapper.class,
                com.fasterxml.jackson.core.JsonFactory.class, com.fasterxml.jackson.annotation.JsonProperty.class,
                com.github.copilot.rpc.ToolDefinition.class};
        for (Class<?> cls : keyClasses) {
            try {
                CodeSource cs = cls.getProtectionDomain().getCodeSource();
                if (cs != null && cs.getLocation() != null) {
                    paths.add(Path.of(cs.getLocation().toURI()).toString());
                }
            } catch (Exception e) {
                // skip this class
            }
        }

        return paths.isEmpty() ? "." : String.join(File.pathSeparator, paths);
    }

    private static JavaFileObject inMemorySource(String className, String code) {
        return new SimpleJavaFileObject(URI.create("string:///" + className.replace('.', '/') + ".java"),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return code;
            }
        };
    }

    private static void assertNoErrors(CompilationResult result) {
        List<Diagnostic<? extends JavaFileObject>> errors = result.diagnostics.stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.ERROR).toList();
        assertTrue(errors.isEmpty(), "Expected no errors, got: " + errors);
    }

    private static boolean hasErrorContaining(CompilationResult result, String substring) {
        return result.diagnostics.stream()
                .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR && d.getMessage(null).contains(substring));
    }

    private static class CompilationResult {
        final List<Diagnostic<? extends JavaFileObject>> diagnostics;
        final List<String> generatedSources;
        final java.nio.file.Path outputDir;

        CompilationResult(List<Diagnostic<? extends JavaFileObject>> diagnostics, List<String> generatedSources,
                java.nio.file.Path outputDir) {
            this.diagnostics = diagnostics;
            this.generatedSources = generatedSources;
            this.outputDir = outputDir;
        }

        String getGeneratedSource(String qualifiedName) {
            String fileName = qualifiedName.replace('.', '/') + ".java";
            java.nio.file.Path filePath = outputDir.resolve(fileName);
            try {
                if (java.nio.file.Files.exists(filePath)) {
                    return java.nio.file.Files.readString(filePath);
                }
            } catch (java.io.IOException e) {
                // fall through
            }
            // Also check in collected sources
            for (String source : generatedSources) {
                if (source.contains(qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1) + "$$CopilotToolMeta")
                        || source.contains("class " + qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1))) {
                    return source;
                }
            }
            return null;
        }
    }
}
