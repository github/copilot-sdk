/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@link CopilotExperimentalProcessor} enforces compile-time gating
 * of experimental APIs at the declaration level.
 */
class CopilotExperimentalProcessorTest {

    private static final String EXPERIMENTAL_TYPE_SOURCE = """
        package test;
        import com.github.copilot.CopilotExperimental;
        @CopilotExperimental
        public class ExperimentalType {
            public void doSomething() {}
        }
        """;

    private static final String EXPERIMENTAL_METHOD_SOURCE = """
        package test;
        import com.github.copilot.CopilotExperimental;
        public class StableType {
            @CopilotExperimental
            public static void experimentalMethod() {}
        }
        """;

    private static final String CONSUMER_USES_TYPE_IN_DECLARATIONS = """
        package consumer;
        import test.ExperimentalType;
        public class Consumer {
            private ExperimentalType field;
            public ExperimentalType getIt() { return field; }
            public void setIt(ExperimentalType value) { this.field = value; }
        }
        """;

    private static final String CONSUMER_EXTENDS_TYPE = """
        package consumer;
        import test.ExperimentalType;
        public class Consumer extends ExperimentalType {
        }
        """;

    @Test
    void failsByDefault_whenFieldOrSignatureUsesExperimentalType() {
        DiagnosticCollector<JavaFileObject> diagnostics = compile(
            List.of(
                inMemorySource("test.ExperimentalType", EXPERIMENTAL_TYPE_SOURCE),
                inMemorySource("consumer.Consumer", CONSUMER_USES_TYPE_IN_DECLARATIONS)
            ),
            Collections.emptyList()
        );

        boolean hasError = diagnostics.getDiagnostics().stream()
            .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR
                && d.getMessage(null).contains("experimental API"));
        assertTrue(hasError, "Expected compile error for experimental type in declarations, got: "
            + diagnostics.getDiagnostics());
    }

    @Test
    void failsByDefault_whenExtendingExperimentalType() {
        DiagnosticCollector<JavaFileObject> diagnostics = compile(
            List.of(
                inMemorySource("test.ExperimentalType", EXPERIMENTAL_TYPE_SOURCE),
                inMemorySource("consumer.Consumer", CONSUMER_EXTENDS_TYPE)
            ),
            Collections.emptyList()
        );

        boolean hasError = diagnostics.getDiagnostics().stream()
            .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR
                && d.getMessage(null).contains("experimental API"));
        assertTrue(hasError, "Expected compile error for extending experimental type, got: "
            + diagnostics.getDiagnostics());
    }

    @Test
    void passes_whenOptInFlagIsProvided() {
        DiagnosticCollector<JavaFileObject> diagnostics = compile(
            List.of(
                inMemorySource("test.ExperimentalType", EXPERIMENTAL_TYPE_SOURCE),
                inMemorySource("test.StableType", EXPERIMENTAL_METHOD_SOURCE),
                inMemorySource("consumer.Consumer", CONSUMER_USES_TYPE_IN_DECLARATIONS)
            ),
            List.of("-Acopilot.experimental.allowed=true")
        );

        boolean hasError = diagnostics.getDiagnostics().stream()
            .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
        assertFalse(hasError, "Expected no errors with opt-in flag, got: "
            + diagnostics.getDiagnostics());
    }

    private DiagnosticCollector<JavaFileObject> compile(
            List<JavaFileObject> sources, List<String> extraOptions) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        String classpath = resolveClasspath();
        List<String> options = new ArrayList<>();
        options.addAll(List.of("-classpath", classpath));
        // Direct output to temp dir to avoid polluting the working directory
        options.addAll(List.of("-d", System.getProperty("java.io.tmpdir")));
        options.addAll(extraOptions);

        JavaCompiler.CompilationTask task = compiler.getTask(
            null, null, diagnostics, options, null, sources);
        task.setProcessors(List.of(new CopilotExperimentalProcessor()));
        task.call();

        return diagnostics;
    }

    /**
     * Resolves the classpath containing {@link CopilotExperimental} so the
     * in-memory compiler can find it. Works in both classpath and module-path
     * environments.
     */
    private static String resolveClasspath() {
        CodeSource cs = CopilotExperimental.class.getProtectionDomain().getCodeSource();
        if (cs != null) {
            URL location = cs.getLocation();
            if (location != null) {
                try {
                    return Path.of(location.toURI()).toString();
                } catch (Exception ignored) {
                    // fall through
                }
            }
        }
        return System.getProperty("java.class.path", ".");
    }

    private static JavaFileObject inMemorySource(String className, String code) {
        return new SimpleJavaFileObject(
            URI.create("string:///" + className.replace('.', '/') + ".java"),
            JavaFileObject.Kind.SOURCE
        ) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return code;
            }
        };
    }
}
