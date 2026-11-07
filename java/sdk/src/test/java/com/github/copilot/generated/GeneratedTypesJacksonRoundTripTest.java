/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.copilot.generated.rpc.SandboxConfig;
import com.github.copilot.generated.rpc.SandboxConfigUserPolicyFilesystem;

/**
 * Reflection-based Jackson round-trip test for all generated types in the
 * {@code com.github.copilot.generated} and
 * {@code com.github.copilot.generated.rpc} packages.
 *
 * <p>
 * Records are deserialized from {@code {}} (empty JSON object) and
 * re-serialized to verify the Jackson annotations work. Enums have every
 * variant serialized and deserialized back via {@code @JsonValue} /
 * {@code @JsonCreator}.
 *
 * <p>
 * This test automatically discovers classes at runtime, so it never needs
 * updating when generated types are added or removed.
 */
class GeneratedTypesJacksonRoundTripTest {

    private static final ObjectMapper MAPPER = createMapper();

    private static final String[] GENERATED_PACKAGES = {"com.github.copilot.generated",
            "com.github.copilot.generated.rpc"};

    private static ObjectMapper createMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

    @Test
    void sandboxConfigLegacyConstructorPreservesRoutingAndWireDefaults() throws IOException {
        var config = new SandboxConfig(true, null, false, true, false, true, false, true, null, null, false);

        assertEquals(true, config.enabled());
        assertEquals(false, config.addCurrentWorkingDirectory());
        assertEquals(true, config.sandboxMcpServers());
        assertEquals(false, config.sandboxLspServers());
        assertEquals(true, config.allowBypass());
        assertEquals(false, config.managedMcpRoutingLocked());
        assertEquals(true, config.managedLspRoutingLocked());
        assertEquals(false, config.allowDevToolAccess());
        assertNull(config.sandboxExtensions());
        assertNull(config.managedExtensionsRoutingLocked());
        var json = MAPPER.readTree(MAPPER.writeValueAsString(config));
        assertFalse(json.has("sandboxExtensions"));
        assertFalse(json.has("managedExtensionsRoutingLocked"));
        assertEquals(config, MAPPER.treeToValue(json, SandboxConfig.class));
    }

    @Test
    void sandboxFilesystemLegacyConstructorOmitsEnumeratePaths() throws IOException {
        var filesystem = new SandboxConfigUserPolicyFilesystem(List.of("rw"), List.of("ro"), List.of("denied"));

        assertEquals(List.of("rw"), filesystem.readwritePaths());
        assertEquals(List.of("ro"), filesystem.readonlyPaths());
        assertEquals(List.of("denied"), filesystem.deniedPaths());
        assertNull(filesystem.enumeratePaths());
        var json = MAPPER.readTree(MAPPER.writeValueAsString(filesystem));
        assertFalse(json.has("enumeratePaths"));
        assertEquals(filesystem, MAPPER.treeToValue(json, SandboxConfigUserPolicyFilesystem.class));
    }

    @Test
    void futureFileEditKindDoesNotRejectToolCompletion() throws IOException {
        var event = MAPPER.readValue(
                "{\"type\":\"tool.execution_complete\",\"data\":{\"toolCallId\":\"call-1\",\"success\":true,"
                        + "\"fileEdits\":[{\"path\":\"/future.txt\",\"kind\":\"append\"}]}}",
                ToolExecutionCompleteEvent.class);

        assertEquals("call-1", event.getData().toolCallId());
        assertEquals("/future.txt", event.getData().fileEdits().get(0).path());
        assertEquals("append", event.getData().fileEdits().get(0).kind().getValue());
        assertEquals("append", MAPPER.readTree(MAPPER.writeValueAsString(event)).path("data").path("fileEdits").get(0)
                .path("kind").asText());
        assertEquals(ToolExecutionCompleteFileEditKind.CREATE, ToolExecutionCompleteFileEditKind.fromValue("create"));
    }

    @TestFactory
    Collection<DynamicTest> roundTripAllGeneratedRecords() throws Exception {
        List<DynamicTest> tests = new ArrayList<>();
        for (Class<?> cls : discoverGeneratedClasses()) {
            if (!cls.isRecord())
                continue;
            tests.add(DynamicTest.dynamicTest("record round-trip: " + cls.getName(), () -> {
                // Deserialize from empty JSON — all fields will be null/default
                Object instance = MAPPER.readValue("{}", cls);
                assertNotNull(instance, "Deserialized instance should not be null for " + cls.getName());

                // Serialize back to JSON
                String json = MAPPER.writeValueAsString(instance);
                assertNotNull(json, "Serialized JSON should not be null for " + cls.getName());

                // Round-trip: deserialize the serialized output
                Object roundTripped = MAPPER.readValue(json, cls);
                assertEquals(instance, roundTripped, "Round-trip should produce equal instance for " + cls.getName());
            }));
        }
        assertFalse(tests.isEmpty(), "Should discover at least one generated record");
        return tests;
    }

    @TestFactory
    Collection<DynamicTest> roundTripAllGeneratedEnums() throws Exception {
        List<DynamicTest> tests = new ArrayList<>();
        for (Class<?> cls : discoverGeneratedClasses()) {
            if (!cls.isEnum())
                continue;
            tests.add(DynamicTest.dynamicTest("enum round-trip: " + cls.getName(), () -> {
                Object[] constants = cls.getEnumConstants();
                assertNotNull(constants, "Enum constants should not be null for " + cls.getName());
                assertTrue(constants.length > 0, "Enum should have at least one constant: " + cls.getName());

                for (Object constant : constants) {
                    // Serialize enum constant to JSON
                    String json = MAPPER.writeValueAsString(constant);
                    assertNotNull(json, "Serialized JSON should not be null for " + constant);

                    // Deserialize back
                    Object deserialized = MAPPER.readValue(json, cls);
                    assertEquals(constant, deserialized,
                            "Round-trip should produce same enum constant for " + constant);
                }
            }));
        }
        assertFalse(tests.isEmpty(), "Should discover at least one generated enum");
        return tests;
    }

    @Test
    void generatedDiscoveryCoversEveryCurrentBuilderFamily() throws Exception {
        List<Class<?>> classes = discoverGeneratedClasses();
        Set<Class<?>> identities = new HashSet<>(classes);
        assertEquals(classes.size(), identities.size(), "Generated binary identities must be discovered exactly once");

        List<Class<?>> records = classes.stream().filter(Class::isRecord).toList();
        List<Class<?>> selectedRecords = records.stream()
                .filter(GeneratedTypesJacksonRoundTripTest::isSelectedBuilderOwner).toList();
        List<Class<?>> nestedSelectedRecords = selectedRecords.stream().filter(type -> type.getDeclaringClass() != null)
                .toList();
        List<Class<?>> enums = classes.stream().filter(Class::isEnum).toList();
        List<Class<?>> mutableVariants = classes.stream().filter(GeneratedTypesJacksonRoundTripTest::isMutableVariant)
                .toList();
        List<Class<?>> sealedVariants = selectedRecords.stream()
                .filter(GeneratedTypesJacksonRoundTripTest::isSealedVariantRecord).toList();
        List<Class<?>> builderOwners = classes.stream()
                .filter(GeneratedTypesJacksonRoundTripTest::isSelectedBuilderOwner).toList();
        assertFalse(records.isEmpty(), "Should discover generated records");
        assertFalse(selectedRecords.isEmpty(), "Should discover selected generated records");
        assertFalse(nestedSelectedRecords.isEmpty(), "Should discover selected nested records");
        assertFalse(enums.isEmpty(), "Should discover generated enums");
        assertFalse(mutableVariants.isEmpty(), "Should discover generated mutable variants");
        assertFalse(sealedVariants.isEmpty(), "Should discover generated sealed variants");
        assertEquals(selectedRecords.size() + mutableVariants.size(), builderOwners.size(),
                "Every selected record and mutable variant should own a builder");
        assertTrue(
                builderOwners.contains(com.github.copilot.generated.rpc.InstallationConfirmationRequestReview.class));
    }

    @Test
    void everySelectedBuilderFactoryAndBuildMethodRetainsExactOwnerIdentity() throws Exception {
        for (Class<?> owner : discoverGeneratedClasses().stream()
                .filter(GeneratedTypesJacksonRoundTripTest::isSelectedBuilderOwner).toList()) {
            var factory = owner.getDeclaredMethod("builder");
            assertTrue(Modifier.isPublic(factory.getModifiers()), owner.getName());
            assertTrue(Modifier.isStatic(factory.getModifiers()), owner.getName());
            Class<?> builderType = factory.getReturnType();
            assertSame(owner, builderType.getDeclaringClass(), owner.getName());
            assertTrue(Modifier.isStatic(builderType.getModifiers()), builderType.getName());
            assertTrue(factory.trySetAccessible(), owner.getName());
            assertSame(builderType, factory.invoke(null).getClass(), owner.getName());
            var build = builderType.getDeclaredMethod("build");
            assertSame(owner, build.getReturnType(), owner.getName());
        }
    }

    private static boolean isSelectedBuilderOwner(Class<?> type) {
        return type.isRecord() ? !hasJsonValue(type) : isMutableVariant(type);
    }

    private static boolean hasJsonValue(Class<?> type) {
        return java.util.Arrays.stream(type.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(JsonValue.class));
    }

    private static boolean isMutableVariant(Class<?> type) {
        Class<?> parent = type.getSuperclass();
        return !type.isRecord() && !type.isEnum() && !Modifier.isAbstract(type.getModifiers())
                && !SessionEvent.class.isAssignableFrom(type) && parent != null
                && parent.isAnnotationPresent(JsonTypeInfo.class);
    }

    private static boolean isSealedVariantRecord(Class<?> type) {
        return java.util.Arrays.stream(type.getInterfaces()).anyMatch(Class::isSealed);
    }

    /**
     * Discovers all classes, including nested types, in the generated packages by
     * scanning compiled {@code .class} files on disk. The packages
     * {@code com.github.copilot.generated} and
     * {@code com.github.copilot.generated.rpc} contain <em>only</em> generated
     * code, so every class identity is included.
     */
    private static List<Class<?>> discoverGeneratedClasses() throws IOException, URISyntaxException {
        List<Class<?>> result = new ArrayList<>();
        for (String pkg : GENERATED_PACKAGES) {
            result.addAll(findClassesInPackage(pkg));
        }
        return result;
    }

    private static List<Class<?>> findClassesInPackage(String packageName) throws IOException, URISyntaxException {
        List<Class<?>> classes = new ArrayList<>();

        // Load a known anchor class from the target package, then derive the
        // compiled .class directory from its code-source location. This works
        // on both JDK 17 (where Class.getResource also works) and JDK 25
        // (where stricter JPMS encapsulation can make Class.getResource
        // return null for classes in named modules).
        String anchorName = packageName + ".AbortReason";
        Class<?> anchor;
        try {
            anchor = Class.forName(anchorName, false, GeneratedTypesJacksonRoundTripTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Anchor class not found: " + anchorName, e);
        }

        URL codeSourceUrl = anchor.getProtectionDomain().getCodeSource().getLocation();
        assertNotNull(codeSourceUrl, "Could not determine code source for " + packageName);
        Path classesRoot = Path.of(codeSourceUrl.toURI());
        Path packageDir = classesRoot.resolve(packageName.replace('.', '/'));
        assertTrue(Files.isDirectory(packageDir), "Expected a directory at " + packageDir);

        try (var files = Files.list(packageDir)) {
            for (Path classFile : files.filter(p -> p.toString().endsWith(".class"))
                    .filter(p -> !p.getFileName().toString().equals("package-info.class")).sorted().toList()) {
                String className = packageName + '.'
                        + packageDir.relativize(classFile).toString().replace('/', '.').replace('\\', '.');
                className = className.substring(0, className.length() - ".class".length());
                try {
                    classes.add(Class.forName(className, false, anchor.getClassLoader()));
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Failed to load generated class " + className, e);
                }
            }
        }
        return classes;
    }
}
