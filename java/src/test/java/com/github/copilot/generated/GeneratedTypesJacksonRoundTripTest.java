/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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

    @TestFactory
    Collection<DynamicTest> roundTripAllGeneratedRecords() {
        List<DynamicTest> tests = new ArrayList<>();
        for (Class<?> cls : discoverGeneratedClasses()) {
            if (!cls.isRecord())
                continue;
            // Skip abstract/sealed event base class — it requires a "type" discriminator
            if (cls == SessionEvent.class)
                continue;
            tests.add(DynamicTest.dynamicTest("record round-trip: " + cls.getSimpleName(), () -> {
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
    Collection<DynamicTest> roundTripAllGeneratedEnums() {
        List<DynamicTest> tests = new ArrayList<>();
        for (Class<?> cls : discoverGeneratedClasses()) {
            if (!cls.isEnum())
                continue;
            tests.add(DynamicTest.dynamicTest("enum round-trip: " + cls.getSimpleName(), () -> {
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

    /**
     * Discovers all top-level classes in the generated packages by scanning
     * compiled {@code .class} files on disk. The packages
     * {@code com.github.copilot.generated} and
     * {@code com.github.copilot.generated.rpc} contain <em>only</em> generated
     * code, so every loadable top-level class is included.
     */
    private static List<Class<?>> discoverGeneratedClasses() {
        List<Class<?>> result = new ArrayList<>();
        for (String pkg : GENERATED_PACKAGES) {
            result.addAll(findClassesInPackage(pkg));
        }
        return result;
    }

    private static List<Class<?>> findClassesInPackage(String packageName) {
        List<Class<?>> classes = new ArrayList<>();

        // Load a known anchor class from the target package, then derive the
        // compiled .class directory from its code-source location. This works
        // on both JDK 17 (where Class.getResource also works) and JDK 25
        // (where stricter JPMS encapsulation can make Class.getResource
        // return null for classes in named modules).
        String anchorName = packageName + ".AbortReason";
        Class<?> anchor;
        try {
            anchor = Class.forName(anchorName);
        } catch (ClassNotFoundException e) {
            fail("Anchor class not found: " + anchorName);
            return classes; // unreachable
        }

        Path packageDir;
        try {
            URL codeSourceUrl = anchor.getProtectionDomain().getCodeSource().getLocation();
            assertNotNull(codeSourceUrl, "Could not determine code source for " + packageName);
            Path classesRoot = Path.of(codeSourceUrl.toURI());
            packageDir = classesRoot.resolve(packageName.replace('.', '/'));
        } catch (URISyntaxException e) {
            fail("Bad URI scanning " + packageName + ": " + e.getMessage());
            return classes; // unreachable
        }
        assertTrue(Files.isDirectory(packageDir), "Expected a directory at " + packageDir);

        try (var files = Files.list(packageDir)) {
            files.filter(p -> p.toString().endsWith(".class")).map(p -> p.getFileName().toString())
                    .filter(name -> !name.contains("$")).forEach(name -> {
                        String className = packageName + '.' + name.substring(0, name.length() - 6);
                        try {
                            classes.add(Class.forName(className));
                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                            // Skip classes that can't be loaded
                        }
                    });
        } catch (IOException e) {
            fail("Failed to scan package " + packageName + ": " + e.getMessage());
        }
        return classes;
    }
}
