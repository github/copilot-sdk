package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class DocumentationSamplesTest {

    private static final Pattern CREATE_SESSION_WITHOUT_PERMISSION = Pattern
            .compile("createSession\\(\\s*new SessionConfig\\(\\)(?!\\.setOnPermissionRequest\\()", Pattern.DOTALL);
    private static final Pattern RESUME_CONFIG_WITHOUT_PERMISSION = Pattern.compile(
            "new ResumeSessionConfig\\(\\)(?!\\.setOnPermissionRequest\\()", Pattern.DOTALL);
    private static final Pattern REMOVED_RESUME_OVERLOAD = Pattern
            .compile("resumeSession\\([^,\\n\\)]*\\)\\.get\\(\\)");

    @Test
    void docsAndJbangSamplesUseRequiredPermissionHandler() throws IOException {
        for (Path path : documentationFiles()) {
            String content = Files.readString(path);
            assertFalse(CREATE_SESSION_WITHOUT_PERMISSION.matcher(content).find(),
                    () -> path + " contains createSession sample without setOnPermissionRequest");
            assertFalse(RESUME_CONFIG_WITHOUT_PERMISSION.matcher(content).find(),
                    () -> path + " contains ResumeSessionConfig sample without setOnPermissionRequest");
            assertFalse(REMOVED_RESUME_OVERLOAD.matcher(content).find(),
                    () -> path + " contains removed resumeSession(String) overload");
        }
    }

    private static List<Path> documentationFiles() throws IOException {
        Path root = Path.of("").toAbsolutePath();
        List<Path> files = new ArrayList<>();
        files.add(root.resolve("README.md"));
        files.add(root.resolve("jbang-example.java"));

        try (Stream<Path> markdownFiles = Files.walk(root.resolve("src/site/markdown"))) {
            markdownFiles.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".md")).forEach(files::add);
        }
        return files;
    }
}
