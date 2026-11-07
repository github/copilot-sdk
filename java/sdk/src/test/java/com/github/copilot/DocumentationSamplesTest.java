package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentationSamplesTest {

    @TempDir
    Path compilationDirectory;

    @Test
    void generatedConstructionExamplesCompile() throws IOException {
        Path root = Path.of("").toAbsolutePath();
        compileMarkdownExample(root.resolve("../README.md"), "ConstructionExample");
        compileMarkdownExample(root.resolve("../README.md"), "ReasoningSummaryExample");
        compileMarkdownExample(root.resolve("../../docs/runtime-supervised-host.md"), "HostingExample");
        compileMarkdownExample(root.resolve("../../docs/features/usage-and-billing.md"), "ContextInfoExample");
    }

    @Test
    void modelCapabilitiesJavadocCompilesWithOwningImports() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/github/copilot/CopilotSession.java"));
        int example = source.indexOf("session.setModel(\"claude-sonnet-4.5\", null,");
        assertTrue(example >= 0, "Missing capability-override Javadoc example");
        int start = source.lastIndexOf("<pre>{@code", example);
        int end = source.indexOf("}</pre>", example);
        assertTrue(start >= 0 && end > start, "Missing Javadoc code delimiters");
        String imports = source.lines().filter(line -> line.startsWith("import ")).collect(Collectors.joining("\n"));
        String code = source.substring(start + "<pre>{@code".length(), end).replaceAll("(?m)^\\s*\\* ?", "");
        compileExample("ModelCapabilitiesExample", imports + """

                class ModelCapabilitiesExample {
                    static void example(com.github.copilot.CopilotSession session) throws Exception {
                """ + code + "\n    }\n}\n");
    }

    private void compileMarkdownExample(Path path, String className) throws IOException {
        var blocks = Pattern.compile("```java\\R(.*?)\\R```", Pattern.DOTALL).matcher(Files.readString(path));
        List<String> examples = new ArrayList<>();
        while (blocks.find()) {
            if (blocks.group(1).contains("class " + className + " ")) {
                examples.add(blocks.group(1));
            }
        }
        assertTrue(examples.size() == 1, () -> path + ": expected exactly one " + className + " example");
        compileExample(className, examples.get(0));
    }

    private void compileExample(String className, String source) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Documentation compilation requires a JDK");
        Path file = compilationDirectory.resolve(className + ".java");
        Files.writeString(file, source);
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = compiler.getStandardFileManager(diagnostics, null, null)) {
            var units = files.getJavaFileObjects(file.toFile());
            String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
            String modulePath = System.getProperty("jdk.module.path");
            if (modulePath != null && !modulePath.isBlank()) {
                classpath += java.io.File.pathSeparator + modulePath;
            }
            var options = List.of("--release", "17", "-proc:none", "-classpath", classpath, "-d",
                    compilationDirectory.toString());
            assertTrue(compiler.getTask(null, files, diagnostics, options, null, units).call(),
                    () -> className + ": " + diagnostics.getDiagnostics());
        }
    }

    @Test
    void docsAndJbangSamplesUseRequiredPermissionHandler() throws IOException {
        for (Path path : documentationFiles()) {
            for (CodeSample sample : codeSamples(path)) {
                String content = stripStringsAndComments(sample.code());
                assertFalse(hasConfigWithoutPermissionHandler(content, "SessionConfig"),
                        () -> sample.location() + " contains SessionConfig sample without setOnPermissionRequest");
                assertFalse(hasConfigWithoutPermissionHandler(content, "ResumeSessionConfig"), () -> sample.location()
                        + " contains ResumeSessionConfig sample without setOnPermissionRequest");
                assertFalse(hasSingleArgumentResumeSessionCall(content),
                        () -> sample.location() + " contains removed resumeSession(String) overload");
            }
        }
    }

    private record CodeSample(String location, String code) {
    }

    /**
     * Returns the fenced code blocks of a Markdown file, each on its own, or the
     * whole content of any other file. Prose is skipped because
     * {@link #stripStringsAndComments} would treat its apostrophes as quotes and
     * hide or expose samples by chance.
     */
    private static List<CodeSample> codeSamples(Path path) throws IOException {
        String content = Files.readString(path);
        if (!path.getFileName().toString().endsWith(".md")) {
            return List.of(new CodeSample(path.toString(), content));
        }
        List<CodeSample> samples = new ArrayList<>();
        List<String> lines = content.lines().toList();
        StringBuilder block = null;
        int blockStart = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.strip().startsWith("```")) {
                if (block == null) {
                    block = new StringBuilder();
                    blockStart = i + 2;
                } else {
                    samples.add(new CodeSample(path + ":" + blockStart, block.toString()));
                    block = null;
                }
            } else if (block != null) {
                block.append(line).append('\n');
            }
        }
        return samples;
    }

    private static boolean hasConfigWithoutPermissionHandler(String content, String configType) {
        String constructor = "new " + configType + "()";
        int fromIndex = 0;
        while (true) {
            int start = content.indexOf(constructor, fromIndex);
            if (start < 0) {
                return false;
            }
            int end = content.indexOf(';', start);
            if (end < 0) {
                end = content.length();
            }
            if (!content.substring(start, end).contains("setOnPermissionRequest(")) {
                return true;
            }
            fromIndex = start + constructor.length();
        }
    }

    private static boolean hasSingleArgumentResumeSessionCall(String content) {
        int fromIndex = 0;
        while (true) {
            int callStart = content.indexOf("resumeSession(", fromIndex);
            if (callStart < 0) {
                return false;
            }
            int index = callStart + "resumeSession(".length();
            int depth = 1;
            int topLevelCommaCount = 0;
            while (index < content.length() && depth > 0) {
                char c = content.charAt(index);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                } else if (c == ',' && depth == 1) {
                    topLevelCommaCount++;
                }
                index++;
            }

            if (depth == 0 && topLevelCommaCount == 0) {
                return true;
            }
            fromIndex = callStart + 1;
        }
    }

    private static String stripStringsAndComments(String input) {
        StringBuilder out = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '"' || c == '\'') {
                char quote = c;
                out.append(' ');
                i++;
                while (i < input.length()) {
                    char current = input.charAt(i);
                    out.append(' ');
                    if (current == '\\') {
                        i++;
                        if (i < input.length()) {
                            out.append(' ');
                        }
                    } else if (current == quote) {
                        i++;
                        break;
                    }
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if (next == '/') {
                    out.append(' ').append(' ');
                    i += 2;
                    while (i < input.length() && input.charAt(i) != '\n') {
                        out.append(' ');
                        i++;
                    }
                    continue;
                }
                if (next == '*') {
                    out.append(' ').append(' ');
                    i += 2;
                    while (i + 1 < input.length() && !(input.charAt(i) == '*' && input.charAt(i + 1) == '/')) {
                        out.append(' ');
                        i++;
                    }
                    if (i + 1 < input.length()) {
                        out.append(' ').append(' ');
                        i += 2;
                    }
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static List<Path> documentationFiles() throws IOException {
        Path root = Path.of("").toAbsolutePath();
        List<Path> files = new ArrayList<>();
        files.add(root.resolve("../README.md"));
        files.add(root.resolve("jbang-example.java"));
        return files;
    }
}
