/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.tool;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SchemaGenerator} using the compilation-testing approach. A
 * test annotation processor exercises SchemaGenerator during compilation of
 * small source snippets.
 */
public class SchemaGeneratorTest {

    /**
     * In-memory Java source file for compilation testing.
     */
    private static class InMemorySource extends SimpleJavaFileObject {

        private final String code;

        InMemorySource(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return code;
        }
    }

    /**
     * Test processor that captures schema generation results.
     */
    @SupportedAnnotationTypes("*")
    @SupportedSourceVersion(SourceVersion.RELEASE_17)
    public static class SchemaCapturingProcessor extends AbstractProcessor {

        static final List<String> capturedSchemas = new ArrayList<>();
        static final List<String> capturedParameterSchemas = new ArrayList<>();

        private Types typeUtils;
        private Elements elementUtils;

        @Override
        public synchronized void init(ProcessingEnvironment processingEnv) {
            super.init(processingEnv);
            this.typeUtils = processingEnv.getTypeUtils();
            this.elementUtils = processingEnv.getElementUtils();
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (roundEnv.processingOver()) {
                return false;
            }

            SchemaGenerator generator = new SchemaGenerator();

            for (Element rootElement : roundEnv.getRootElements()) {
                if (rootElement.getKind() == ElementKind.CLASS || rootElement.getKind() == ElementKind.RECORD
                        || rootElement.getKind() == ElementKind.INTERFACE
                        || rootElement.getKind() == ElementKind.ENUM) {
                    // Find methods named "schemaTarget" to capture schemas for their return type
                    for (Element enclosed : rootElement.getEnclosedElements()) {
                        if (enclosed.getKind() == ElementKind.METHOD) {
                            ExecutableElement method = (ExecutableElement) enclosed;
                            String methodName = method.getSimpleName().toString();
                            if (methodName.startsWith("schemaTarget")) {
                                TypeMirror returnType = method.getReturnType();
                                String schema = generator.generateSchemaSource(returnType, typeUtils, elementUtils);
                                capturedSchemas.add(methodName + "=" + schema);
                            }
                            if ("parametersTarget".equals(methodName)) {
                                List<? extends VariableElement> params = method.getParameters();
                                String schema = generator.generateParametersSchemaSource(params, typeUtils,
                                        elementUtils);
                                capturedParameterSchemas.add(schema);
                            }
                        }
                    }

                    // For record/enum types, generate schema for the type itself
                    TypeElement typeElement = (TypeElement) rootElement;
                    String typeName = typeElement.getSimpleName().toString();
                    if (typeName.startsWith("TestRecord") || typeName.startsWith("TestEnum")
                            || typeName.startsWith("TestSealed")) {
                        String schema = generator.generateSchemaSource(typeElement.asType(), typeUtils, elementUtils);
                        capturedSchemas.add(typeName + "=" + schema);
                    }
                }
            }

            return false;
        }
    }

    private List<String> compileAndCapture(String... sources) {
        return compileAndCapture(Arrays.asList(sources));
    }

    private List<String> compileAndCapture(List<String> sourceTexts) {
        SchemaCapturingProcessor.capturedSchemas.clear();
        SchemaCapturingProcessor.capturedParameterSchemas.clear();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "System Java compiler not available");

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        List<JavaFileObject> compilationUnits = new ArrayList<>();
        for (String sourceText : sourceTexts) {
            // Extract class name from source
            String className = extractClassName(sourceText);
            compilationUnits.add(new InMemorySource(className, sourceText));
        }

        // Compile with the processor on classpath
        JavaCompiler.CompilationTask task = compiler.getTask(null, // writer
                null, // file manager
                diagnostics, // diagnostics
                List.of("--add-modules", "ALL-MODULE-PATH"), // options
                null, // annotation classes
                compilationUnits);

        task.setProcessors(List.of(new SchemaCapturingProcessor()));
        boolean success = task.call();

        if (!success) {
            // Try without module options for simpler environments
            diagnostics = new DiagnosticCollector<>();
            task = compiler.getTask(null, null, diagnostics, null, null, compilationUnits);
            task.setProcessors(List.of(new SchemaCapturingProcessor()));
            success = task.call();
        }

        assertTrue(success, "Compilation failed: " + diagnostics.getDiagnostics());
        return new ArrayList<>(SchemaCapturingProcessor.capturedSchemas);
    }

    private List<String> compileAndCaptureParams(String source) {
        SchemaCapturingProcessor.capturedSchemas.clear();
        SchemaCapturingProcessor.capturedParameterSchemas.clear();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "System Java compiler not available");

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        String className = extractClassName(source);
        List<JavaFileObject> compilationUnits = List.of(new InMemorySource(className, source));

        JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics, null, null, compilationUnits);
        task.setProcessors(List.of(new SchemaCapturingProcessor()));
        boolean success = task.call();

        assertTrue(success, "Compilation failed: " + diagnostics.getDiagnostics());
        return new ArrayList<>(SchemaCapturingProcessor.capturedParameterSchemas);
    }

    private String extractClassName(String source) {
        // Simple extraction: find "class X", "record X", "enum X", or "interface X"
        for (String keyword : new String[]{"class ", "record ", "enum ", "interface "}) {
            int idx = source.indexOf(keyword);
            if (idx >= 0) {
                int start = idx + keyword.length();
                int end = start;
                while (end < source.length() && Character.isJavaIdentifierPart(source.charAt(end))) {
                    end++;
                }
                return source.substring(start, end);
            }
        }
        return "Unknown";
    }

    // --- Type mapping tests ---

    @Test
    void stringType() {
        String source = """
                public class TestStringHolder {
                    public String schemaTargetString() { return null; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetString", "Map.of(\"type\", \"string\")");
    }

    @Test
    void intPrimitiveType() {
        String source = """
                public class TestIntHolder {
                    public int schemaTargetInt() { return 0; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetInt", "Map.of(\"type\", \"integer\")");
    }

    @Test
    void integerBoxedType() {
        String source = """
                public class TestIntegerHolder {
                    public Integer schemaTargetInteger() { return null; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetInteger", "Map.of(\"type\", \"integer\")");
    }

    @Test
    void longType() {
        String source = """
                public class TestLongHolder {
                    public long schemaTargetLong() { return 0L; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetLong", "Map.of(\"type\", \"integer\")");
    }

    @Test
    void doubleType() {
        String source = """
                public class TestDoubleHolder {
                    public double schemaTargetDouble() { return 0.0; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetDouble", "Map.of(\"type\", \"number\")");
    }

    @Test
    void floatType() {
        String source = """
                public class TestFloatHolder {
                    public float schemaTargetFloat() { return 0.0f; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetFloat", "Map.of(\"type\", \"number\")");
    }

    @Test
    void booleanPrimitiveType() {
        String source = """
                public class TestBooleanHolder {
                    public boolean schemaTargetBoolean() { return false; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetBoolean", "Map.of(\"type\", \"boolean\")");
    }

    @Test
    void booleanBoxedType() {
        String source = """
                public class TestBooleanBoxedHolder {
                    public Boolean schemaTargetBooleanBoxed() { return null; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetBooleanBoxed", "Map.of(\"type\", \"boolean\")");
    }

    @Test
    void stringArrayType() {
        String source = """
                public class TestArrayHolder {
                    public String[] schemaTargetArray() { return null; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetArray",
                "Map.of(\"type\", \"array\", \"items\", Map.of(\"type\", \"string\"))");
    }

    @Test
    void enumType() {
        String source = """
                public enum TestEnumColor { RED, GREEN, BLUE }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "TestEnumColor",
                "Map.of(\"type\", \"string\", \"enum\", List.of(\"RED\", \"GREEN\", \"BLUE\"))");
    }

    @Test
    void listOfStringType() {
        String source = """
                import java.util.List;
                public class TestListHolder {
                    public List<String> schemaTargetList() { return null; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetList",
                "Map.of(\"type\", \"array\", \"items\", Map.of(\"type\", \"string\"))");
    }

    @Test
    void mapStringStringType() {
        String source = """
                import java.util.Map;
                public class TestMapHolder {
                    public Map<String, String> schemaTargetMap() { return null; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetMap",
                "Map.of(\"type\", \"object\", \"additionalProperties\", Map.of(\"type\", \"string\"))");
    }

    @Test
    void mapStringObjectType() {
        String source = """
                import java.util.Map;
                public class TestMapObjectHolder {
                    public Map<String, Object> schemaTargetMapObject() { return null; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetMapObject", "Map.of(\"type\", \"object\")");
    }

    @Test
    void mapStringBooleanType() {
        String source = """
                import java.util.Map;
                public class TestMapBoolHolder {
                    public Map<String, Boolean> schemaTargetMapBool() { return null; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetMapBool",
                "Map.of(\"type\", \"object\", \"additionalProperties\", Map.of(\"type\", \"boolean\"))");
    }

    @Test
    void mapStringLongType() {
        String source = """
                import java.util.Map;
                public class TestMapLongHolder {
                    public Map<String, Long> schemaTargetMapLong() { return null; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetMapLong",
                "Map.of(\"type\", \"object\", \"additionalProperties\", Map.of(\"type\", \"integer\"))");
    }

    @Test
    void optionalStringType() {
        String source = """
                import java.util.Optional;
                public class TestOptionalHolder {
                    public Optional<String> schemaTargetOptional() { return null; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetOptional", "Map.of(\"type\", \"string\")");
    }

    @Test
    void uuidType() {
        String source = """
                import java.util.UUID;
                public class TestUuidHolder {
                    public UUID schemaTargetUuid() { return null; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetUuid", "Map.of(\"type\", \"string\", \"format\", \"uuid\")");
    }

    @Test
    void offsetDateTimeType() {
        String source = """
                import java.time.OffsetDateTime;
                public class TestDateTimeHolder {
                    public OffsetDateTime schemaTargetDateTime() { return null; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetDateTime",
                "Map.of(\"type\", \"string\", \"format\", \"date-time\")");
    }

    @Test
    void recordType() {
        String source = """
                public record TestRecordPerson(String name, int age, boolean active) {}
                """;
        List<String> schemas = compileAndCapture(source);
        String expected = "Map.of(\"type\", \"object\", \"properties\", "
                + "Map.of(\"name\", Map.of(\"type\", \"string\"), " + "\"age\", Map.of(\"type\", \"integer\"), "
                + "\"active\", Map.of(\"type\", \"boolean\")), "
                + "\"required\", List.of(\"name\", \"age\", \"active\"))";
        assertContainsSchema(schemas, "TestRecordPerson", expected);
    }

    @Test
    void recordWithOptionalField() {
        String source = """
                import java.util.Optional;
                public record TestRecordWithOptional(String name, Optional<String> nickname) {}
                """;
        List<String> schemas = compileAndCapture(source);
        String expected = "Map.of(\"type\", \"object\", \"properties\", "
                + "Map.of(\"name\", Map.of(\"type\", \"string\"), " + "\"nickname\", Map.of(\"type\", \"string\")), "
                + "\"required\", List.of(\"name\"))";
        assertContainsSchema(schemas, "TestRecordWithOptional", expected);
    }

    @Test
    void parametersSchema() {
        String source = """
                public class TestParamsHolder {
                    public void parametersTarget(String query, int limit, boolean verbose) {}
                }
                """;
        List<String> paramSchemas = compileAndCaptureParams(source);
        assertFalse(paramSchemas.isEmpty(), "Expected parameter schemas");
        String schema = paramSchemas.get(0);
        assertTrue(schema.contains("\"type\", \"object\""), "Should be object type: " + schema);
        assertTrue(schema.contains("\"query\", Map.of(\"type\", \"string\")"), "Should have query property: " + schema);
        assertTrue(schema.contains("\"limit\", Map.of(\"type\", \"integer\")"),
                "Should have limit property: " + schema);
        assertTrue(schema.contains("\"verbose\", Map.of(\"type\", \"boolean\")"),
                "Should have verbose property: " + schema);
        assertTrue(schema.contains("\"required\", List.of("), "Should have required list: " + schema);
    }

    @Test
    void generatedSourceIsValidJava() {
        // Verify that generated schema source code compiles when embedded in a method
        // body
        String source = """
                import java.util.List;
                import java.util.Map;
                import java.util.Optional;
                public class TestValidJavaHolder {
                    public String schemaTargetStr() { return null; }
                    public List<String> schemaTargetListStr() { return null; }
                    public Map<String, String> schemaTargetMapStr() { return null; }
                    public Optional<String> schemaTargetOpt() { return null; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertFalse(schemas.isEmpty());

        // Build a Java source that uses the generated schema expressions
        StringBuilder validationSource = new StringBuilder();
        validationSource.append("import java.util.Map;\n");
        validationSource.append("import java.util.List;\n");
        validationSource.append("public class SchemaValidation {\n");
        validationSource.append("    @SuppressWarnings(\"unchecked\")\n");
        validationSource.append("    public void validate() {\n");
        for (int i = 0; i < schemas.size(); i++) {
            String schema = schemas.get(i);
            String schemaExpr = schema.substring(schema.indexOf('=') + 1);
            validationSource.append("        Object s" + i + " = " + schemaExpr + ";\n");
        }
        validationSource.append("    }\n");
        validationSource.append("}\n");

        // Compile the validation source to verify syntactic validity
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<JavaFileObject> compilationUnits = List
                .of(new InMemorySource("SchemaValidation", validationSource.toString()));

        JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics, null, null, compilationUnits);
        boolean success = task.call();

        assertTrue(success, "Generated schema source code is not valid Java: " + diagnostics.getDiagnostics()
                + "\nSource:\n" + validationSource);
    }

    @Test
    void nestedMapListType() {
        String source = """
                import java.util.List;
                import java.util.Map;
                public class TestNestedHolder {
                    public Map<String, List<String>> schemaTargetNestedMap() { return null; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        String expected = "Map.of(\"type\", \"object\", \"additionalProperties\", "
                + "Map.of(\"type\", \"array\", \"items\", Map.of(\"type\", \"string\")))";
        assertContainsSchema(schemas, "schemaTargetNestedMap", expected);
    }

    @Test
    void objectType() {
        String source = """
                public class TestObjectHolder {
                    public Object schemaTargetObject() { return null; }
                }
                """;
        List<String> schemas = compileAndCapture(source);
        assertContainsSchema(schemas, "schemaTargetObject", "Map.of()");
    }

    private void assertContainsSchema(List<String> schemas, String methodName, String expectedSchema) {
        String expected = methodName + "=" + expectedSchema;
        assertTrue(schemas.stream().anyMatch(s -> s.equals(expected)),
                "Expected schema '" + expected + "' not found in: " + schemas);
    }
}
