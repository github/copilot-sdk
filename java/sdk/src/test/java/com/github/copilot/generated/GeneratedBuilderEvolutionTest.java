/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Arrays;
import java.util.jar.JarFile;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeneratedBuilderEvolutionTest {
    private static final String BASELINE_RESOURCE = "/generated-builder-evolution-baseline/";
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path tempDirectory;

    @Test
    @Tag("java-compatibility")
    void publishedGeneratedTypesRetainPublicConstructorsAndBuilderOwners() throws Exception {
        Path jar = releasedJar();
        try (JarFile release = new JarFile(jar.toFile()); URLClassLoader loader = releaseClassLoader(jar)) {
            List<String> names = release.stream().map(java.util.jar.JarEntry::getName)
                    .filter(name -> name.startsWith("com/github/copilot/generated/") && name.endsWith(".class"))
                    .map(name -> name.substring(0, name.length() - 6).replace('/', '.')).sorted().toList();
            assertFalse(names.isEmpty(), "Published baseline contains no generated classes");
            List<String> missing = new ArrayList<>();
            for (String name : names) {
                Class<?> previous = Class.forName(name, false, loader);
                if (!Modifier.isPublic(previous.getModifiers()))
                    continue;
                Class<?> current = Class.forName(name);
                Set<List<String>> constructors = Arrays.stream(current.getConstructors())
                        .map(constructor -> parameterNames(constructor.getParameterTypes()))
                        .collect(Collectors.toSet());
                for (var constructor : previous.getConstructors()) {
                    List<String> parameters = parameterNames(constructor.getParameterTypes());
                    if (!constructors.contains(parameters))
                        missing.add(name + parameters);
                }
                if (previous.isRecord() && Arrays.stream(previous.getDeclaredMethods()).noneMatch(
                        method -> method.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonValue.class))) {
                    assertSame(current, current.getDeclaredMethod("builder").getReturnType().getDeclaredMethod("build")
                            .getReturnType(), name);
                }
            }
            assertTrue(missing.isEmpty(),
                    "Missing constructors from com.github:copilot-sdk-java:1.0.28:\n" + String.join("\n", missing));
        }
    }

    @Test
    @Tag("java-compatibility")
    void publishedSessionOpenOptionsConstructorPreservesNeighboringFields() throws Exception {
        try (URLClassLoader loader = releaseClassLoader(releasedJar())) {
            Class<?> previous = Class.forName("com.github.copilot.generated.rpc.SessionOpenOptions", false, loader);
            List<String> signature = parameterNames(previous.getConstructors()[0].getParameterTypes());
            var constructor = Arrays.stream(com.github.copilot.generated.rpc.SessionOpenOptions.class.getConstructors())
                    .filter(candidate -> parameterNames(candidate.getParameterTypes()).equals(signature)).findFirst()
                    .orElseThrow(() -> new AssertionError("Published SessionOpenOptions constructor missing"));
            Object[] arguments = Arrays.stream(previous.getRecordComponents())
                    .map(component -> switch (component.getName()) {
                        case "enableStreaming" -> true;
                        case "enableCitations" -> false;
                        case "copilotUrl" -> "legacy-url";
                        default -> null;
                    }).toArray();
            assertEquals(
                    JSON.readTree("{\"enableStreaming\":true,\"enableCitations\":false,\"copilotUrl\":\"legacy-url\"}"),
                    JSON.valueToTree(constructor.newInstance(arguments)));
        }
    }

    @Test
    @Tag("java-compatibility")
    void releasedConsumerBytecodeLinksAndPreservesConstructorArgumentMeanings() throws Exception {
        Path jar = releasedJar();
        Path source = Path.of(GeneratedBuilderEvolutionTest.class
                .getResource("/generated-builder-evolution/consumers/ReleasedConstructorCaller.java").toURI());
        Path caller = tempDirectory.resolve("released-consumer");
        Compilation compilation = compile(requireCompiler(), List.of(source), caller, prependClasspath(jar));
        assertTrue(compilation.success(), compilation.diagnostics());
        Path bytecode = caller.resolve("fixture/ReleasedConstructorCaller.class");
        String before = sha256(bytecode);
        String[] output = run("fixture.ReleasedConstructorCaller", caller,
                Path.of(com.github.copilot.generated.rpc.TaskBlocker.class.getProtectionDomain().getCodeSource()
                        .getLocation().toURI()))
                .strip().split("\\R");
        assertEquals(7, output.length);
        JsonNode skill = JSON.readTree(output[0]);
        assertEquals("example", skill.path("name").asText());
        assertEquals("content", skill.path("content").asText());
        assertEquals("tool", skill.path("allowedTools").get(0).asText());
        assertEquals(5, skill.path("invokedAtTurn").asInt());
        assertFalse(skill.has("arguments"));
        JsonNode blocker = JSON.readTree(output[1]);
        assertEquals("permission_required", blocker.path("reason").asText());
        assertTrue(blocker.path("resumable").asBoolean());
        JsonNode filesystem = JSON.readTree(output[2]);
        assertEquals("/write", filesystem.path("readwritePaths").get(0).asText());
        assertEquals("/read", filesystem.path("readonlyPaths").get(0).asText());
        assertEquals("/deny", filesystem.path("deniedPaths").get(0).asText());
        assertFalse(filesystem.has("clearPolicyOnExit"),
                "The runtime's retired, ignored setting must not be reintroduced into current schemas");
        JsonNode metadata = JSON.readTree(output[3]);
        assertEquals("session", metadata.path("sessionId").asText());
        assertEquals("start", metadata.path("startTime").asText());
        assertEquals("modified", metadata.path("modifiedTime").asText());
        assertEquals("client", metadata.path("clientName").asText());
        assertFalse(metadata.path("isRemote").asBoolean());
        assertTrue(metadata.path("isDetached").asBoolean());
        assertEquals("task", metadata.path("mcTaskId").asText());
        assertFalse(metadata.has("lastClientName"));
        JsonNode account = JSON.readTree(output[4]);
        assertEquals("session", account.path("sessionId").asText());
        assertEquals("switchActive", account.path("command").path("kind").asText());
        assertEquals("selected", account.path("command").path("selectionId").asText());
        JsonNode command = JSON.readTree(output[5]);
        assertEquals("legacy-request", command.path("requestId").asText());
        assertEquals("/help", command.path("command").asText());
        assertFalse(command.has("scheduled"));
        JsonNode marketplace = JSON.readTree(output[6]);
        assertEquals("market", marketplace.path("name").asText());
        assertEquals("source", marketplace.path("source").asText());
        assertFalse(marketplace.path("isDefault").asBoolean());
        assertTrue(marketplace.path("managed").asBoolean());
        assertFalse(marketplace.path("available").asBoolean());
        assertFalse(marketplace.has("team"));
        assertEquals(before, sha256(bytecode));
    }

    private static List<String> parameterNames(Class<?>[] parameters) {
        return Arrays.stream(parameters).map(Class::getName).toList();
    }

    private static URLClassLoader releaseClassLoader(Path jar) throws Exception {
        return new URLClassLoader(new URL[]{jar.toUri().toURL()},
                GeneratedBuilderEvolutionTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.startsWith("com.github.copilot."))
                    return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name)) {
                    Class<?> type = findLoadedClass(name);
                    if (type == null)
                        type = findClass(name);
                    if (resolve)
                        resolveClass(type);
                    return type;
                }
            }
        };
    }

    private static Path releasedJar() throws Exception {
        String directory = System.getProperty("copilot.java.compat.baseline.dir");
        require(directory != null, "Run with -Pjava-compatibility to prepare the published baseline");
        Path root = Path.of(directory);
        Path jar = root.resolve("copilot-sdk-java-1.0.28.jar");
        assertEquals("64312b5674d6bfa964e2f69e01341fd5ef73e80d37db641ba32ca5fc75cb3031", sha256(jar),
                "Published baseline JAR differs");
        assertEquals("ebdb025cf84405132f60ecf869bd4483c46463303f87cd35a82f670220655220",
                sha256(root.resolve("copilot-sdk-java-1.0.28.pom")), "Published baseline POM differs");
        return jar;
    }

    @Test
    @Tag("java-compatibility")
    void positionalRpcSourceAndSavedBytecodeExecuteAgainstGeneratedDtos() throws Exception {
        verifyPositionalCaller("rpc", "fixture.PositionalRpcCaller", "request-42|baseline-note");
    }

    @Test
    @Tag("java-compatibility")
    void positionalNestedEventSourceAndSavedBytecodeExecuteAgainstGeneratedDtos() throws Exception {
        verifyPositionalCaller("events", "fixture.PositionalEventCaller", "baseline-message|7|true|[tag]|note");
    }

    @Test
    @Tag("java-compatibility")
    void rpcBuildersPreserveSourceAndBytecodeAcrossOptionalPropertyEvolution() throws Exception {
        JavaCompiler compiler = requireCompiler();
        Path baseline = baselineDirectory();
        Path generated = generatedDirectory();
        verifyGeneratedFixtures(generated);

        Path callerSource = generated.resolve("consumers/BuilderRpcCaller.java");
        assertTrue(Files.isRegularFile(callerSource), "Missing builder caller source: " + callerSource);

        Path noBuilderApi = tempDirectory.resolve("rpc-no-builder-api");
        Compilation baselineCompilation = compile(compiler, javaSources(baseline.resolve("rpc")), noBuilderApi,
                testClasspath());
        assertTrue(baselineCompilation.success(), baselineCompilation.diagnostics());
        Compilation noBuilderCaller = compile(compiler, List.of(callerSource),
                tempDirectory.resolve("rpc-no-builder-caller"), prependClasspath(noBuilderApi));
        assertFalse(noBuilderCaller.success(), noBuilderCaller.diagnostics());
        assertTrue(noBuilderCaller.diagnostics().contains("builder()"), noBuilderCaller.diagnostics());
        assertTrue(noBuilderCaller.diagnostics().contains("cannot find symbol"), noBuilderCaller.diagnostics());

        Path apiA = tempDirectory.resolve("rpc-evolution-api-a");
        Compilation apiACompilation = compile(compiler, javaSources(generated.resolve("rpc-evolution/a")), apiA,
                testClasspath());
        assertTrue(apiACompilation.success(), apiACompilation.diagnostics());

        Path savedCaller = tempDirectory.resolve("rpc-evolution-saved-caller");
        Compilation callerACompilation = compile(compiler, List.of(callerSource), savedCaller, prependClasspath(apiA));
        assertTrue(callerACompilation.success(), callerACompilation.diagnostics());
        Path savedBytecode = savedCaller.resolve("fixture/BuilderRpcCaller.class");
        assertTrue(Files.isRegularFile(savedBytecode), "Compiler did not produce builder caller bytecode");
        String savedBytecodeHash = sha256(savedBytecode);
        assertEquals("{\"requestId\":\"request-42\"}",
                JSON.readTree(run("fixture.BuilderRpcCaller", savedCaller, apiA)).toString());

        Path apiB = tempDirectory.resolve("rpc-evolution-api-b");
        Compilation apiBCompilation = compile(compiler, javaSources(generated.resolve("rpc-evolution/optional")), apiB,
                testClasspath());
        assertTrue(apiBCompilation.success(), apiBCompilation.diagnostics());

        Path recompiledCaller = tempDirectory.resolve("rpc-evolution-recompiled-caller");
        Compilation callerBCompilation = compile(compiler, List.of(callerSource), recompiledCaller,
                prependClasspath(apiB));
        assertTrue(callerBCompilation.success(), callerBCompilation.diagnostics());
        assertEquals("{\"requestId\":\"request-42\"}",
                JSON.readTree(run("fixture.BuilderRpcCaller", recompiledCaller, apiB)).toString());

        Path addedPropertySource = generated.resolve("consumers/BuilderRpcAddedPropertyCaller.java");
        Path addedPropertyCaller = tempDirectory.resolve("rpc-evolution-added-property-caller");
        Compilation addedPropertyCompilation = compile(compiler, List.of(addedPropertySource), addedPropertyCaller,
                prependClasspath(apiB));
        assertTrue(addedPropertyCompilation.success(), addedPropertyCompilation.diagnostics());
        String[] jsonLines = run("fixture.BuilderRpcAddedPropertyCaller", addedPropertyCaller, apiB).strip()
                .split("\\R");
        assertEquals(2, jsonLines.length);
        assertEquals("added", JSON.readTree(jsonLines[0]).path("insertedOptional").asText());
        assertFalse(JSON.readTree(jsonLines[1]).has("insertedOptional"));

        assertEquals("{\"requestId\":\"request-42\"}",
                JSON.readTree(run("fixture.BuilderRpcCaller", savedCaller, apiB)).toString());
        assertEquals(savedBytecodeHash, sha256(savedBytecode),
                "Saved A caller bytecode changed while executing against B DTOs");

        Path apiRequired = tempDirectory.resolve("rpc-evolution-api-required");
        Compilation apiRequiredCompilation = compile(compiler, javaSources(generated.resolve("rpc-evolution/required")),
                apiRequired, testClasspath());
        assertTrue(apiRequiredCompilation.success(), apiRequiredCompilation.diagnostics());
        Path requiredCaller = tempDirectory.resolve("rpc-evolution-required-caller");
        Compilation requiredCallerCompilation = compile(compiler, List.of(callerSource), requiredCaller,
                prependClasspath(apiRequired));
        assertTrue(requiredCallerCompilation.success(), requiredCallerCompilation.diagnostics());
        assertEquals("Missing required property: requiredAfterA",
                run("fixture.BuilderRpcCaller", requiredCaller, apiRequired).strip());
    }

    @Test
    @Tag("java-compatibility")
    void eventBuildersPreserveSourceAndBytecodeAcrossNestedPayloadEvolution() throws Exception {
        JavaCompiler compiler = requireCompiler();
        Path baseline = baselineDirectory();
        Path generated = generatedDirectory();
        verifyGeneratedFixtures(generated);

        Path callerSource = generated.resolve("consumers/BuilderEventCaller.java");
        assertTrue(Files.isRegularFile(callerSource), "Missing event builder caller source: " + callerSource);

        Path noBuilderApi = tempDirectory.resolve("event-no-builder-api");
        Compilation baselineCompilation = compile(compiler, javaSources(baseline.resolve("events")), noBuilderApi,
                testClasspath());
        assertTrue(baselineCompilation.success(), baselineCompilation.diagnostics());
        Compilation noBuilderCaller = compile(compiler, List.of(callerSource),
                tempDirectory.resolve("event-no-builder-caller"), prependClasspath(noBuilderApi));
        assertFalse(noBuilderCaller.success(), noBuilderCaller.diagnostics());
        assertTrue(noBuilderCaller.diagnostics().contains("builder()"), noBuilderCaller.diagnostics());
        assertTrue(noBuilderCaller.diagnostics().contains("cannot find symbol"), noBuilderCaller.diagnostics());

        Path apiA = tempDirectory.resolve("event-evolution-api-a");
        Compilation apiACompilation = compile(compiler, javaSources(generated.resolve("event-evolution/a")), apiA,
                testClasspath());
        assertTrue(apiACompilation.success(), apiACompilation.diagnostics());

        Path savedCaller = tempDirectory.resolve("event-evolution-saved-caller");
        Compilation callerACompilation = compile(compiler, List.of(callerSource), savedCaller, prependClasspath(apiA));
        assertTrue(callerACompilation.success(), callerACompilation.diagnostics());
        Path savedBytecode = savedCaller.resolve("fixture/BuilderEventCaller.class");
        assertTrue(Files.isRegularFile(savedBytecode), "Compiler did not produce event builder caller bytecode");
        String savedBytecodeHash = sha256(savedBytecode);
        assertEquals("{\"message\":\"baseline-message\",\"sequence\":7,\"active\":true,\"tags\":[\"tag\"]}",
                JSON.readTree(run("fixture.BuilderEventCaller", savedCaller, apiA)).toString());

        Path apiB = tempDirectory.resolve("event-evolution-api-b");
        Compilation apiBCompilation = compile(compiler, javaSources(generated.resolve("event-evolution/optional")),
                apiB, testClasspath());
        assertTrue(apiBCompilation.success(), apiBCompilation.diagnostics());

        Path recompiledCaller = tempDirectory.resolve("event-evolution-recompiled-caller");
        Compilation callerBCompilation = compile(compiler, List.of(callerSource), recompiledCaller,
                prependClasspath(apiB));
        assertTrue(callerBCompilation.success(), callerBCompilation.diagnostics());
        assertEquals("{\"message\":\"baseline-message\",\"sequence\":7,\"active\":true,\"tags\":[\"tag\"]}",
                JSON.readTree(run("fixture.BuilderEventCaller", recompiledCaller, apiB)).toString());

        Path addedPropertySource = generated.resolve("consumers/BuilderEventAddedPropertyCaller.java");
        Path addedPropertyCaller = tempDirectory.resolve("event-evolution-added-property-caller");
        Compilation addedPropertyCompilation = compile(compiler, List.of(addedPropertySource), addedPropertyCaller,
                prependClasspath(apiB));
        assertTrue(addedPropertyCompilation.success(), addedPropertyCompilation.diagnostics());
        String[] jsonLines = run("fixture.BuilderEventAddedPropertyCaller", addedPropertyCaller, apiB).strip()
                .split("\\R");
        assertEquals(2, jsonLines.length);
        assertEquals("added", JSON.readTree(jsonLines[0]).path("insertedOptional").asText());
        assertFalse(JSON.readTree(jsonLines[1]).has("insertedOptional"));

        assertEquals("{\"message\":\"baseline-message\",\"sequence\":7,\"active\":true,\"tags\":[\"tag\"]}",
                JSON.readTree(run("fixture.BuilderEventCaller", savedCaller, apiB)).toString());
        assertEquals(savedBytecodeHash, sha256(savedBytecode),
                "Saved A event caller bytecode changed while executing against B DTOs");

        Path apiRequired = tempDirectory.resolve("event-evolution-api-required");
        Compilation apiRequiredCompilation = compile(compiler,
                javaSources(generated.resolve("event-evolution/required")), apiRequired, testClasspath());
        assertTrue(apiRequiredCompilation.success(), apiRequiredCompilation.diagnostics());
        Path requiredCaller = tempDirectory.resolve("event-evolution-required-caller");
        Compilation requiredCallerCompilation = compile(compiler, List.of(callerSource), requiredCaller,
                prependClasspath(apiRequired));
        assertTrue(requiredCallerCompilation.success(), requiredCallerCompilation.diagnostics());
        assertEquals("Missing required property: requiredAfterA",
                run("fixture.BuilderEventCaller", requiredCaller, apiRequired).strip());
    }

    @Test
    @Tag("java-compatibility")
    void eventBuildersValidateRequiredPresenceWithoutChangingJacksonNullBehavior() throws Exception {
        JavaCompiler compiler = requireCompiler();
        Path generated = generatedDirectory();
        verifyGeneratedFixtures(generated);
        Path api = tempDirectory.resolve("event-builder-validation-api");
        Compilation apiCompilation = compile(compiler, javaSources(generated.resolve("event-validation")), api,
                testClasspath());
        assertTrue(apiCompilation.success(), apiCompilation.diagnostics());

        Path caller = tempDirectory.resolve("event-builder-validation-caller");
        Compilation callerCompilation = compile(compiler,
                List.of(generated.resolve("consumers/BuilderEventValidationCaller.java")), caller,
                prependClasspath(api));
        assertTrue(callerCompilation.success(), callerCompilation.diagnostics());
        String[] output = run("fixture.BuilderEventValidationCaller", caller, api).strip().split("\\R");
        assertEquals("Missing required property: message", output[0]);
        assertEquals("present", JSON.readTree(output[1]).path("message").asText());
        assertFalse(JSON.readTree(output[1]).has("requiredNullable"));
    }

    @Test
    @Tag("java-compatibility")
    void nestedAndEmptyEventPayloadBuildersCompileAndRoundTripValues() throws Exception {
        JavaCompiler compiler = requireCompiler();
        Path generated = generatedDirectory();
        verifyGeneratedFixtures(generated);

        Path emptyApi = tempDirectory.resolve("event-empty-coverage-api");
        Compilation emptyApiCompilation = compile(compiler, javaSources(generated.resolve("event-empty-coverage")),
                emptyApi, testClasspath());
        assertTrue(emptyApiCompilation.success(), emptyApiCompilation.diagnostics());
        Path emptyCaller = tempDirectory.resolve("event-empty-coverage-caller");
        Compilation emptyCallerCompilation = compile(compiler,
                List.of(generated.resolve("consumers/BuilderEventEmptyCaller.java")), emptyCaller,
                prependClasspath(emptyApi));
        assertTrue(emptyCallerCompilation.success(), emptyCallerCompilation.diagnostics());
        assertEquals("{}", JSON.readTree(run("fixture.BuilderEventEmptyCaller", emptyCaller, emptyApi)).toString());

        Path nestedApi = tempDirectory.resolve("event-nested-coverage-api");
        Compilation nestedApiCompilation = compile(compiler, javaSources(generated.resolve("event-nested-coverage")),
                nestedApi, testClasspath());
        assertTrue(nestedApiCompilation.success(), nestedApiCompilation.diagnostics());
        Path nestedCaller = tempDirectory.resolve("event-nested-coverage-caller");
        Compilation nestedCallerCompilation = compile(compiler,
                List.of(generated.resolve("consumers/BuilderEventNestedCaller.java")), nestedCaller,
                prependClasspath(nestedApi));
        assertTrue(nestedCallerCompilation.success(), nestedCallerCompilation.diagnostics());
        assertEquals(
                "{\"profile\":{\"name\":\"Ada\",\"child\":{\"value\":\"leaf\"}},\"items\":[{\"id\":\"entry\"}],\"lookup\":{\"key\":{\"count\":3}}}",
                JSON.readTree(run("fixture.BuilderEventNestedCaller", nestedCaller, nestedApi)).toString());
    }

    @Test
    @Tag("java-compatibility")
    void rpcBuilderValidatesPresenceAndRetainsNullAndValueSerialization() throws Exception {
        JavaCompiler compiler = requireCompiler();
        Path generated = generatedDirectory();
        verifyGeneratedFixtures(generated);
        Path api = tempDirectory.resolve("rpc-builder-validation-api");
        Compilation apiCompilation = compile(compiler, javaSources(generated.resolve("validation")), api,
                testClasspath());
        assertTrue(apiCompilation.success(), apiCompilation.diagnostics());

        Path caller = tempDirectory.resolve("rpc-builder-validation-caller");
        Compilation callerCompilation = compile(compiler,
                List.of(generated.resolve("consumers/BuilderValidationCaller.java")), caller, prependClasspath(api));
        assertTrue(callerCompilation.success(), callerCompilation.diagnostics());
        JsonNode json = JSON.readTree(run("fixture.BuilderValidationCaller", caller, api));
        assertFalse(json.path("enabled").asBoolean());
        assertEquals(0, json.path("count").asLong());
        assertEquals("", json.path("emptyString").asText());
        assertTrue(json.path("tags").isArray());
        assertEquals(0, json.path("tags").size());
        assertEquals(JSON.readTree("null"), json.get("requiredNullable"));
        assertFalse(json.has("optionalDefault"));
    }

    @Test
    void rpcBuilderRejectsMissingRequiredInputWithoutRejectingEmptyOrFalseValues() throws Exception {
        var missing = assertThrows(IllegalStateException.class,
                () -> com.github.copilot.generated.rpc.SessionModelSwitchToParams.builder().build());
        assertEquals("Missing required property: modelId", missing.getMessage());
        var switchModel = com.github.copilot.generated.rpc.SessionModelSwitchToParams.builder().modelId("")
                .requireAvailable(false).build();
        assertEquals("", switchModel.modelId());
        assertEquals(Boolean.FALSE, switchModel.requireAvailable());

        var value = com.github.copilot.generated.rpc.SessionOptionsUpdateParams.builder().sessionId("").model("")
                .isExperimentalMode(false).availableTools(List.of()).build();
        var json = JSON.readTree(JSON.writeValueAsString(value));
        assertEquals("", json.path("sessionId").asText());
        assertEquals("", json.path("model").asText());
        assertEquals(JSON.readTree("false"), json.path("isExperimentalMode"));
        assertEquals(JSON.readTree("[]"), json.path("availableTools"));
        assertFalse(json.has("featureFlags"));
        assertEquals(value, JSON.treeToValue(json, com.github.copilot.generated.rpc.SessionOptionsUpdateParams.class));
    }

    @Test
    void nestedEventBuildersRetainZeroAndOmittedOptionalProperties() throws Exception {
        var limits = SessionLimitsConfig.builder().maxAiCredits(0.0).build();
        var payload = SessionSessionLimitsChangedEvent.SessionSessionLimitsChangedEventData.builder()
                .sessionLimits(limits).build();
        var json = JSON.readTree(JSON.writeValueAsString(payload));
        assertEquals(JSON.readTree("0.0"), json.path("sessionLimits").path("maxAiCredits"));
        assertEquals(payload,
                JSON.treeToValue(json, SessionSessionLimitsChangedEvent.SessionSessionLimitsChangedEventData.class));
        assertEquals("{}", JSON.writeValueAsString(SessionLimitsConfig.builder().build()));
    }

    @Test
    void requiredNullableBuilderPresenceIsPrivateAndJacksonRemainsTolerant() throws Exception {
        var missing = assertThrows(IllegalStateException.class,
                () -> SessionSessionLimitsChangedEvent.SessionSessionLimitsChangedEventData.builder().build());
        assertEquals("Missing required property: sessionLimits", missing.getMessage());

        var explicitlyNull = SessionSessionLimitsChangedEvent.SessionSessionLimitsChangedEventData.builder()
                .sessionLimits(null).build();
        assertNull(explicitlyNull.sessionLimits());
        assertNull(JSON.readValue("{}", SessionSessionLimitsChangedEvent.SessionSessionLimitsChangedEventData.class)
                .sessionLimits());
        assertNull(JSON.readValue("{\"sessionLimits\":null,\"futureField\":true}",
                SessionSessionLimitsChangedEvent.SessionSessionLimitsChangedEventData.class).sessionLimits());
        assertEquals("{}", JSON.readTree(JSON.writeValueAsString(explicitlyNull)).toString());
    }

    @Test
    void builderReuseKeepsValueAssignmentsIndependent() {
        var builder = com.github.copilot.generated.rpc.SessionModelSwitchToParams.builder().sessionId("session-1")
                .modelId("model-a");
        var first = builder.build();
        var second = builder.modelId("model-b").build();
        var equivalent = com.github.copilot.generated.rpc.SessionModelSwitchToParams.builder().sessionId("session-1")
                .modelId("model-a").build();

        assertNotSame(first, second);
        assertEquals("model-a", first.modelId());
        assertEquals("model-b", second.modelId());
        assertEquals(first, equivalent);
        assertEquals(first.hashCode(), equivalent.hashCode());
        assertTrue(first.toString().contains("modelId=model-a"), first.toString());
    }

    @Test
    void builderReusePreservesShallowCollectionAliasingAndNullElements() {
        var builder = com.github.copilot.generated.rpc.SessionAgentListResult.builder();
        var firstItems = new ArrayList<com.github.copilot.generated.rpc.AgentInfo>();
        firstItems.add(null);
        var first = builder.agents(firstItems).build();
        var secondItems = new ArrayList<com.github.copilot.generated.rpc.AgentInfo>();
        var second = builder.agents(secondItems).build();
        firstItems.add(null);
        var equivalent = com.github.copilot.generated.rpc.SessionAgentListResult.builder()
                .agents(new ArrayList<>(firstItems)).build();

        assertSame(firstItems, first.agents());
        assertSame(secondItems, second.agents());
        assertNotSame(first.agents(), second.agents());
        assertEquals(2, first.agents().size());
        assertNull(first.agents().get(0));
        assertEquals(first, equivalent);
        assertEquals(first.hashCode(), equivalent.hashCode());
        assertTrue(first.toString().contains("agents="), first.toString());
    }

    @Test
    void representativeRpcBuildersRoundTripThroughJackson() throws Exception {
        var switchTo = com.github.copilot.generated.rpc.SessionModelSwitchToParams.builder().sessionId("session-1")
                .modelId("model-1").build();
        var options = com.github.copilot.generated.rpc.SessionOptionsUpdateParams.builder().sessionId("session-1")
                .model("model-2").build();

        var switchJson = JSON.writeValueAsString(switchTo);
        var optionsJson = JSON.writeValueAsString(options);
        assertEquals("model-1", JSON
                .readValue(switchJson, com.github.copilot.generated.rpc.SessionModelSwitchToParams.class).modelId());
        assertEquals("model-2",
                JSON.readValue(optionsJson, com.github.copilot.generated.rpc.SessionOptionsUpdateParams.class).model());
        assertFalse(JSON.readTree(switchJson).has("autoTier"));
        assertFalse(JSON.readTree(optionsJson).has("featureFlags"));
    }

    @Test
    void sealedAndMutableUnionVariantBuildersKeepJacksonDiscriminators() throws Exception {
        var sealed = com.github.copilot.generated.rpc.McpPlanTransportChoicePackage.builder().choiceId("choice-1")
                .transport(com.github.copilot.generated.rpc.McpPlanPackageTransport.STDIO).packageType("npm")
                .packageIdentifier("example/package").requiredValues(List.of()).secretPlaceholders(List.of()).build();
        assertEquals("package", sealed.installMethod());
        assertThrows(NoSuchMethodException.class,
                () -> sealed.getClass().getDeclaredMethod("installMethod", String.class));
        String sealedJson = JSON.writeValueAsString(sealed);
        var parsedSealed = JSON.readValue(sealedJson, com.github.copilot.generated.rpc.McpPlanTransportChoice.class);
        assertInstanceOf(com.github.copilot.generated.rpc.McpPlanTransportChoicePackage.class, parsedSealed);
        assertEquals("example/package",
                ((com.github.copilot.generated.rpc.McpPlanTransportChoicePackage) parsedSealed).packageIdentifier());

        var mutable = com.github.copilot.generated.rpc.McpInstallationResultCatalogInvalidRequestError.builder()
                .field(com.github.copilot.generated.rpc.CatalogInvalidRequestField.PAGE).message("invalid page")
                .build();
        assertEquals("invalid-request", mutable.getKind());
        String mutableJson = JSON.writeValueAsString(mutable);
        var parsedMutable = JSON.readValue(mutableJson, com.github.copilot.generated.rpc.McpInstallationResult.class);
        assertInstanceOf(com.github.copilot.generated.rpc.McpInstallationResultCatalogInvalidRequestError.class,
                parsedMutable);
        var typedMutable = (com.github.copilot.generated.rpc.McpInstallationResultCatalogInvalidRequestError) parsedMutable;
        assertEquals(com.github.copilot.generated.rpc.CatalogInvalidRequestField.PAGE, typedMutable.getField());
        assertEquals("invalid page", typedMutable.getMessage());
    }

    @Test
    void mutableVariantBuilderPreservesMutableObjectSemantics() {
        var mutable = com.github.copilot.generated.rpc.McpInstallationResultCatalogInvalidRequestError.builder()
                .field(com.github.copilot.generated.rpc.CatalogInvalidRequestField.PAGE).message("invalid page")
                .build();
        var sameValues = com.github.copilot.generated.rpc.McpInstallationResultCatalogInvalidRequestError.builder()
                .field(com.github.copilot.generated.rpc.CatalogInvalidRequestField.PAGE).message("invalid page")
                .build();

        assertFalse(mutable.getClass().isRecord());
        assertNotEquals(mutable, sameValues);
        mutable.setMessage("updated page");
        assertEquals("updated page", mutable.getMessage());
    }

    @Test
    @Tag("java-compatibility")
    void compilerFailureRetainsTheCompleteExpectedDiagnostic() throws Exception {
        JavaCompiler compiler = requireCompiler();
        Path source = tempDirectory.resolve("diagnostics/MissingTypeControl.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "package fixture; final class MissingTypeControl { MissingBaselineType value; }\n",
                StandardCharsets.UTF_8);
        Path output = tempDirectory.resolve("diagnostics/classes");
        Compilation result = compile(compiler, List.of(source), output, testClasspath());

        assertFalse(result.success(), result.diagnostics());
        assertTrue(result.diagnostics().contains("MissingBaselineType"), result.diagnostics());
        assertTrue(result.diagnostics().contains("compiler.err.cant.resolve"), result.diagnostics());
    }

    @Test
    @Tag("java-compatibility")
    void generatedFixtureManifestRejectsStaleAndMissingSources() throws Exception {
        Path generated = generatedDirectory();
        verifyGeneratedFixtures(generated);

        Path stale = tempDirectory.resolve("stale");
        copyTree(generated, stale);
        Path staleSource = firstGeneratedSource(stale);
        Files.writeString(staleSource, "\n", StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
        IllegalStateException staleFailure = assertThrows(IllegalStateException.class,
                () -> verifyGeneratedFixtures(stale));
        assertTrue(staleFailure.getMessage().contains("SHA-256 mismatch"), staleFailure.getMessage());

        Path missing = tempDirectory.resolve("missing");
        copyTree(generated, missing);
        Path missingSource = firstGeneratedSource(missing);
        String missingRelative = manifestPath(missing.relativize(missingSource));
        Files.delete(missingSource);
        IllegalStateException missingFailure = assertThrows(IllegalStateException.class,
                () -> verifyGeneratedFixtures(missing));
        assertTrue(missingFailure.getMessage().contains(missingRelative), missingFailure.getMessage());
    }

    private void verifyPositionalCaller(String family, String mainClass, String expectedOutput) throws Exception {
        JavaCompiler compiler = requireCompiler();
        Path baseline = baselineDirectory();
        Path generated = generatedDirectory();
        verifyGeneratedFixtures(generated);

        Path baselineApi = tempDirectory.resolve(family + "-baseline-api");
        List<Path> baselineSources = javaSources(baseline.resolve(family));
        Compilation baselineCompilation = compile(compiler, baselineSources, baselineApi, testClasspath());
        assertTrue(baselineCompilation.success(), baselineCompilation.diagnostics());

        Path callerSource = baseline.resolve("consumers")
                .resolve(mainClass.substring(mainClass.lastIndexOf('.') + 1) + ".java");
        assertTrue(Files.isRegularFile(callerSource), "Missing positional caller source: " + callerSource);
        Path callerClasses = tempDirectory.resolve(family + "-saved-caller");
        Compilation callerCompilation = compile(compiler, List.of(callerSource), callerClasses,
                prependClasspath(baselineApi));
        assertTrue(callerCompilation.success(), callerCompilation.diagnostics());

        Path callerBytecode = callerClasses.resolve(mainClass.replace('.', '/') + ".class");
        assertTrue(Files.isRegularFile(callerBytecode), "Compiler did not produce caller bytecode");
        String savedBytecodeHash = sha256(callerBytecode);
        assertEquals(expectedOutput, run(mainClass, callerClasses, baselineApi).strip());

        Path currentApi = tempDirectory.resolve(family + "-generated-api");
        Compilation currentCompilation = compile(compiler, javaSources(generated.resolve(family)), currentApi,
                testClasspath());
        assertTrue(currentCompilation.success(), currentCompilation.diagnostics());

        assertEquals(expectedOutput, run(mainClass, callerClasses, currentApi).strip());
        assertEquals(savedBytecodeHash, sha256(callerBytecode), "Saved caller bytecode changed after execution");

        Path currentCallerClasses = tempDirectory.resolve(family + "-current-caller");
        Compilation currentCallerCompilation = compile(compiler, List.of(callerSource), currentCallerClasses,
                prependClasspath(currentApi));
        assertTrue(currentCallerCompilation.success(), currentCallerCompilation.diagnostics());
        assertEquals(expectedOutput, run(mainClass, currentCallerClasses, currentApi).strip());
    }

    private static Compilation compile(JavaCompiler compiler, List<Path> sources, Path outputDirectory,
            String classpath) throws IOException {
        require(!sources.isEmpty(), "Compiler source set is empty");
        Files.createDirectories(outputDirectory);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StringWriter compilerOutput = new StringWriter();
        List<String> options = List.of("--release", "17", "-proc:none", "-encoding", "UTF-8", "-classpath", classpath,
                "-d", outputDirectory.toString());
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null,
                StandardCharsets.UTF_8)) {
            var compilationUnits = fileManager.getJavaFileObjectsFromPaths(sources);
            boolean success = Boolean.TRUE.equals(compiler.getTask(new java.io.PrintWriter(compilerOutput), fileManager,
                    diagnostics, options, null, compilationUnits).call());
            return new Compilation(success, formatDiagnostics(diagnostics, compilerOutput));
        }
    }

    private static String run(String mainClass, Path callerClasses, Path apiClasses) throws Exception {
        String classpath = Stream.of(callerClasses, apiClasses).map(Path::toString)
                .collect(Collectors.joining(java.io.File.pathSeparator));
        classpath += java.io.File.pathSeparator + testClasspath();
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-classpath", classpath, mainClass).start();
        boolean finished = process.waitFor(15, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("Java consumer timed out: " + mainClass);
        }
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(),
                "Consumer failed: " + mainClass + "\nstdout:\n" + stdout + "\nstderr:\n" + stderr);
        return stdout;
    }

    private static void verifyGeneratedFixtures(Path root) throws IOException {
        require(Files.isDirectory(root), "Generated compatibility fixture directory is missing: " + root);
        JsonNode manifest = readJson(root.resolve("manifest.json"));
        require(manifest.path("schemaSha256").asText().matches("[0-9a-f]{64}"),
                "Generated fixture schema digest is missing");
        require(manifest.path("rendererSha256").asText().matches("[0-9a-f]{64}"),
                "Generated fixture renderer digest is missing");
        Set<String> expectedRendererSources = Set.of("java/scripts/codegen/java.ts",
                "java/scripts/codegen/rpc-variant-owners.ts", "scripts/codegen/extensible-enums.ts",
                "scripts/codegen/legacy-parameters.ts", "scripts/codegen/protocol-type-roots.ts");
        Set<String> rendererSources = new TreeSet<>();
        for (JsonNode source : manifest.path("rendererSources")) {
            require(source.path("sha256").asText().matches("[0-9a-f]{64}"),
                    "Generated fixture renderer source digest is missing");
            rendererSources.add(source.path("path").asText());
        }
        require(expectedRendererSources.equals(rendererSources), "Generated fixture renderer sources differ; expected "
                + expectedRendererSources + ", actual " + rendererSources);
        require(manifest.path("files").size() > 0, "Generated fixture manifest contains no source files");

        Set<String> expectedFiles = new TreeSet<>();
        expectedFiles.add("manifest.json");
        for (JsonNode file : manifest.path("files")) {
            String relative = file.path("path").asText();
            Path source = root.resolve(relative).normalize();
            require(source.startsWith(root.normalize()), "Generated fixture path escapes its root: " + relative);
            require(Files.isRegularFile(source), "Generated fixture is missing: " + relative);
            require(file.path("sha256").asText().equals(sha256(source)),
                    "Generated fixture SHA-256 mismatch: " + relative);
            expectedFiles.add(relative);
        }
        Set<String> actualFiles;
        try (Stream<Path> paths = Files.walk(root)) {
            actualFiles = paths.filter(Files::isRegularFile).map(root::relativize)
                    .map(GeneratedBuilderEvolutionTest::manifestPath).collect(Collectors.toCollection(TreeSet::new));
        }
        require(expectedFiles.equals(actualFiles),
                "Generated fixture file set differs; expected " + expectedFiles + ", actual " + actualFiles);
    }

    private static JsonNode readJson(Path path) throws IOException {
        require(Files.isRegularFile(path), "Required compatibility manifest is missing: " + path);
        return JSON.readTree(path.toFile());
    }

    private static JavaCompiler requireCompiler() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        require(compiler != null, "A full JDK with the system Java compiler is required");
        return compiler;
    }

    private static List<Path> javaSources(Path root) throws IOException {
        require(Files.isDirectory(root), "Java fixture source directory is missing: " + root);
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> sources = paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .sorted().toList();
            require(!sources.isEmpty(), "Java fixture source set is empty: " + root);
            return sources;
        }
    }

    private static String manifestPath(Path path) {
        return path.toString().replace(path.getFileSystem().getSeparator(), "/");
    }

    private static Path firstGeneratedSource(Path root) throws IOException {
        return javaSources(root.resolve("events")).get(0);
    }

    private static Path baselineDirectory() throws Exception {
        URI uri = GeneratedBuilderEvolutionTest.class.getResource(BASELINE_RESOURCE).toURI();
        return Path.of(uri);
    }

    private static Path generatedDirectory() {
        String directory = System.getProperty("copilot.java.compat.fixtures.dir");
        require(directory != null && !directory.isBlank(),
                "Maven must generate Java compatibility fixtures before running this test");
        return Path.of(directory).toAbsolutePath().normalize();
    }

    private static String prependClasspath(Path first) {
        return first + java.io.File.pathSeparator + testClasspath();
    }

    private static String testClasspath() {
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        String modulePath = System.getProperty("jdk.module.path");
        return modulePath == null || modulePath.isBlank()
                ? classpath
                : classpath + java.io.File.pathSeparator + modulePath;
    }

    private static String formatDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics,
            StringWriter compilerOutput) {
        List<String> messages = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            String source = diagnostic.getSource() == null ? "<unknown>" : diagnostic.getSource().getName();
            messages.add(String.format("%s:%d:%d: %s [%s] %s", source, diagnostic.getLineNumber(),
                    diagnostic.getColumnNumber(), diagnostic.getKind(), diagnostic.getCode(),
                    diagnostic.getMessage(null)));
        }
        if (!compilerOutput.toString().isEmpty())
            messages.add("compiler output:\n" + compilerOutput);
        return String.join("\n", messages);
    }

    private static void copyTree(Path source, Path destination) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path target = destination.resolve(source.relativize(path));
                if (Files.isDirectory(path))
                    Files.createDirectories(target);
                else
                    Files.copy(path, target);
            }
        }
    }

    private static String sha256(Path path) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JDK", e);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition)
            throw new IllegalStateException(message);
    }

    private record Compilation(boolean success, String diagnostics) {
    }

}
