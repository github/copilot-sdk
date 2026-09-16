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
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    private static final String CLASS_ANNOTATED_CONSUMER = """
            package consumer;
            import com.github.copilot.AllowCopilotExperimental;
            import test.ExperimentalType;
            @AllowCopilotExperimental
            public class Consumer extends ExperimentalType {
                private ExperimentalType field;
                public ExperimentalType getIt() { return field; }
                public void setIt(ExperimentalType value) { this.field = value; }
            }
            """;

    private static final String METHOD_ANNOTATED_CONSUMER = """
            package consumer;
            import com.github.copilot.AllowCopilotExperimental;
            import test.ExperimentalType;
            public class Consumer {
                @AllowCopilotExperimental
                public ExperimentalType getIt(ExperimentalType value) {
                    return value;
                }
            }
            """;

    private static final String CONNECTOR_MCP_PUBLIC_PRIMITIVES_CONSUMER = """
            package consumer;

            import java.util.Map;

            import com.github.copilot.CopilotSession;
            import com.github.copilot.generated.SessionMcpServerNeedsReconnectEvent;
            import com.github.copilot.generated.SessionMcpServerRemovedEvent;
            import com.github.copilot.generated.SessionMcpServerStatusChangedEvent;
            import com.github.copilot.generated.SessionMcpServersLoadedEvent;
            import com.github.copilot.generated.rpc.SessionMcpDisableParams;
            import com.github.copilot.generated.rpc.SessionMcpEnableParams;
            import com.github.copilot.generated.rpc.SessionMcpListToolsParams;
            import com.github.copilot.rpc.ConnectorMcpServerConfig;
            import com.github.copilot.rpc.ResumeSessionConfig;
            import com.github.copilot.rpc.SessionConfig;

            public class Consumer {
                public void compose(CopilotSession session, ConnectorMcpServerConfig server) {
                    String serverKey = "calendar";
                    server.setAuthorizationCacheTtlMs(60_000L);
                    var servers = Map.of(serverKey, server);
                    new SessionConfig().setConnectorMcpServers(servers);
                    new ResumeSessionConfig().setConnectorMcpServers(servers);
                    session.getRpc().mcp.list();
                    session.getRpc().mcp.enable(new SessionMcpEnableParams(null, serverKey));
                    session.getRpc().mcp.disable(new SessionMcpDisableParams(null, serverKey));
                    session.getRpc().mcp.listTools(new SessionMcpListToolsParams(null, serverKey));
                    session.on(SessionMcpServersLoadedEvent.class, event -> {});
                    session.on(SessionMcpServerStatusChangedEvent.class, event -> {});
                    session.on(SessionMcpServerRemovedEvent.class, event -> {});
                    session.on(SessionMcpServerNeedsReconnectEvent.class, event -> {});
                }
            }
            """;

    @Test
    void failsByDefault_whenFieldOrSignatureUsesExperimentalType() {
        DiagnosticCollector<JavaFileObject> diagnostics = compile(
                List.of(inMemorySource("test.ExperimentalType", EXPERIMENTAL_TYPE_SOURCE),
                        inMemorySource("consumer.Consumer", CONSUMER_USES_TYPE_IN_DECLARATIONS)),
                Collections.emptyList());

        boolean hasError = diagnostics.getDiagnostics().stream()
                .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR && d.getMessage(null).contains("experimental API"));
        assertTrue(hasError,
                "Expected compile error for experimental type in declarations, got: " + diagnostics.getDiagnostics());
    }

    @Test
    void failsByDefault_whenExtendingExperimentalType() {
        DiagnosticCollector<JavaFileObject> diagnostics = compile(
                List.of(inMemorySource("test.ExperimentalType", EXPERIMENTAL_TYPE_SOURCE),
                        inMemorySource("consumer.Consumer", CONSUMER_EXTENDS_TYPE)),
                Collections.emptyList());

        boolean hasError = diagnostics.getDiagnostics().stream()
                .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR && d.getMessage(null).contains("experimental API"));
        assertTrue(hasError,
                "Expected compile error for extending experimental type, got: " + diagnostics.getDiagnostics());
    }

    @Test
    void passes_whenAllowAnnotationIsOnType() {
        DiagnosticCollector<JavaFileObject> diagnostics = compile(
                List.of(inMemorySource("test.ExperimentalType", EXPERIMENTAL_TYPE_SOURCE),
                        inMemorySource("consumer.Consumer", CLASS_ANNOTATED_CONSUMER)),
                Collections.emptyList());

        boolean hasError = diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
        assertFalse(hasError, "Expected no errors with type-level opt-in, got: " + diagnostics.getDiagnostics());
    }

    @Test
    void passes_whenAllowAnnotationIsOnMethod() {
        DiagnosticCollector<JavaFileObject> diagnostics = compile(
                List.of(inMemorySource("test.ExperimentalType", EXPERIMENTAL_TYPE_SOURCE),
                        inMemorySource("consumer.Consumer", METHOD_ANNOTATED_CONSUMER)),
                Collections.emptyList());

        boolean hasError = diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
        assertFalse(hasError, "Expected no errors with method-level opt-in, got: " + diagnostics.getDiagnostics());
    }

    @Test
    void passes_whenOptInFlagIsProvided() {
        DiagnosticCollector<JavaFileObject> diagnostics = compile(
                List.of(inMemorySource("test.ExperimentalType", EXPERIMENTAL_TYPE_SOURCE),
                        inMemorySource("test.StableType", EXPERIMENTAL_METHOD_SOURCE),
                        inMemorySource("consumer.Consumer", CONSUMER_USES_TYPE_IN_DECLARATIONS)),
                List.of("-Acopilot.experimental.allowed=true"));

        boolean hasError = diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
        assertFalse(hasError, "Expected no errors with opt-in flag, got: " + diagnostics.getDiagnostics());
    }

    @Test
    void connectorMcpServerApisArePublicAndExperimentalForCreateAndResume() {
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var checkedMethods = new AtomicInteger();
        var task = ToolProvider.getSystemJavaCompiler().getTask(null, null, diagnostics,
                List.of("-classpath", resolveClasspath(), "-proc:only"), null,
                List.of(inMemorySource("consumer.Consumer", "package consumer; public class Consumer {}")));
        task.setProcessors(List.of(new AbstractProcessor() {
            @Override
            public Set<String> getSupportedAnnotationTypes() {
                return Set.of("*");
            }

            @Override
            public SourceVersion getSupportedSourceVersion() {
                return SourceVersion.latestSupported();
            }

            @Override
            public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
                if (!roundEnv.processingOver()) {
                    var connectorConfig = processingEnv.getElementUtils()
                            .getTypeElement("com.github.copilot.rpc.ConnectorMcpServerConfig");
                    assertNotNull(connectorConfig);
                    assertTrue(connectorConfig.getModifiers().contains(Modifier.PUBLIC));
                    var connectorMethodNames = ElementFilter.methodsIn(connectorConfig.getEnclosedElements()).stream()
                            .map(method -> method.getSimpleName().toString())
                            .collect(java.util.stream.Collectors.toSet());
                    assertTrue(connectorMethodNames.contains("getAuthorizationCacheTtlMs"));
                    assertTrue(connectorMethodNames.contains("setAuthorizationCacheTtlMs"));
                    assertFalse(connectorMethodNames.contains("getHeadersRefreshTtlMs"));
                    assertFalse(connectorMethodNames.contains("setHeadersRefreshTtlMs"));

                    for (String configName : List.of("SessionConfig", "ResumeSessionConfig")) {
                        var config = processingEnv.getElementUtils()
                                .getTypeElement("com.github.copilot.rpc." + configName);
                        assertNotNull(config);
                        var methodNames = ElementFilter.methodsIn(config.getEnclosedElements()).stream()
                                .map(method -> method.getSimpleName().toString())
                                .collect(java.util.stream.Collectors.toSet());
                        assertFalse(methodNames.contains("getManagedMcpServers"));
                        assertFalse(methodNames.contains("setManagedMcpServers"));
                        assertFalse(methodNames.contains("getCatalogMcpServers"));
                        assertFalse(methodNames.contains("setCatalogMcpServers"));
                        for (var method : ElementFilter.methodsIn(config.getEnclosedElements())) {
                            if (Set.of("getConnectorMcpServers", "setConnectorMcpServers")
                                    .contains(method.getSimpleName().toString())) {
                                assertNotNull(method.getAnnotation(CopilotExperimental.class),
                                        configName + "." + method.getSimpleName());
                                checkedMethods.incrementAndGet();
                            }
                        }
                    }

                }
                return false;
            }
        }));

        assertTrue(task.call(), () -> "Compiler diagnostics: " + diagnostics.getDiagnostics());
        assertEquals(4, checkedMethods.get());
    }

    @Test
    void connectorMcpConfigurationComposesWithPublicSessionRpcAndStatusEvents() {
        var diagnostics = compile(
                List.of(inMemorySource("consumer.Consumer", CONNECTOR_MCP_PUBLIC_PRIMITIVES_CONSUMER)),
                List.of("-Acopilot.experimental.allowed=true"));

        boolean hasError = diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
        assertFalse(hasError, "Expected public MCP primitives to compile, got: " + diagnostics.getDiagnostics());
    }

    private DiagnosticCollector<JavaFileObject> compile(List<JavaFileObject> sources, List<String> extraOptions) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        String classpath = resolveClasspath();
        List<String> options = new ArrayList<>();
        options.addAll(List.of("-classpath", classpath));
        // Direct output to temp dir to avoid polluting the working directory
        options.addAll(List.of("-d", System.getProperty("java.io.tmpdir")));
        options.addAll(extraOptions);

        JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics, options, null, sources);
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
        return new SimpleJavaFileObject(URI.create("string:///" + className.replace('.', '/') + ".java"),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return code;
            }
        };
    }
}
