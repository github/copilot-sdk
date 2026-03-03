package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class DocumentationSamplesTest {

    @Test
    void docsAndJbangSamplesUseRequiredPermissionHandler() throws IOException {
        for (Path path : documentationFiles()) {
            String content = stripStringsAndComments(Files.readString(path));
            assertFalse(hasConfigWithoutPermissionHandler(content, "SessionConfig"),
                    () -> path + " contains SessionConfig sample without setOnPermissionRequest");
            assertFalse(hasConfigWithoutPermissionHandler(content, "ResumeSessionConfig"),
                    () -> path + " contains ResumeSessionConfig sample without setOnPermissionRequest");
            assertFalse(hasSingleArgumentResumeSessionCall(content),
                    () -> path + " contains removed resumeSession(String) overload");
        }
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
        files.add(root.resolve("README.md"));
        files.add(root.resolve("jbang-example.java"));

        try (Stream<Path> markdownFiles = Files.walk(root.resolve("src/site/markdown"))) {
            markdownFiles.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".md"))
                    .forEach(files::add);
        }
        return files;
    }
}
